/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/

package org.pih.warehouse.inventory

import grails.converters.JSON
import org.grails.plugins.csv.CSVWriter
import org.pih.warehouse.api.StockMovement
import org.pih.warehouse.api.StockMovementDirection
import org.pih.warehouse.api.StockMovementItem
import org.pih.warehouse.core.ActivityCode
import org.pih.warehouse.core.BulkDocumentCommand
import org.pih.warehouse.core.Constants
import org.pih.warehouse.core.Document
import org.pih.warehouse.core.DocumentCommand
import org.pih.warehouse.core.DocumentType
import org.pih.warehouse.core.Location
import org.pih.warehouse.importer.CSVUtils
import org.pih.warehouse.importer.ImportDataCommand
import org.pih.warehouse.order.Order
import org.pih.warehouse.picklist.PicklistItem
import org.pih.warehouse.requisition.RequisitionSourceType
import org.pih.warehouse.requisition.RequisitionStatus
import org.pih.warehouse.shipping.Shipment
import org.pih.warehouse.shipping.ShipmentStatusCode

class StockMovementController {

    def dataService
    def stockMovementService
    def outboundStockMovementService
    def shipmentService

    // This template is generated by webpack during application start
    def index = {
        redirect(action: "create", params: params)
    }

    def create = {
        StockMovementDirection stockMovementDirection = params.direction as StockMovementDirection
        if (stockMovementDirection == StockMovementDirection.INBOUND) {
            redirect(action: "createInbound")
        }
        else {
            redirect(action: "createOutbound")
        }
    }

    def createOutbound = {
        render(template: "/common/react", params: params)
    }

    def createInbound = {
        render(template: "/common/react", params: params)
    }

    def createRequest = {
        render(template: "/common/react", params: params)
    }

    def verifyRequest = {
        render(template: "/common/react", params: params)
    }

    def createCombinedShipments = {
        render(template: "/common/react", params: params)
    }

    def edit = {
        Location currentLocation = Location.get(session.warehouse.id)
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)

        if(!stockMovement.isEditAuthorized(currentLocation)) {
            flash.error = stockMovementService.getDisabledMessage(stockMovement, currentLocation, true)
            redirect(controller: "stockMovement", action: "show", id: params.id)
            return
        }

        if(stockMovement.isReturn) {
            redirect(controller: "stockTransfer", action: "edit", params: params)
        } else if (stockMovement?.getStockMovementDirection(currentLocation) == StockMovementDirection.OUTBOUND && stockMovement?.requisition?.sourceType == RequisitionSourceType.ELECTRONIC) {
            redirect(action: "verifyRequest", params: params)
        }
        else if (stockMovement?.getStockMovementDirection(currentLocation) == StockMovementDirection.INBOUND) {

            if (stockMovement.isFromOrder) {
                redirect(action: "createCombinedShipments", params: params)
            } else if (stockMovement.requisition?.sourceType == RequisitionSourceType.ELECTRONIC) {
                if (stockMovement.requisition?.status == RequisitionStatus.CREATED) {
                    redirect(action: "createRequest", params: params)
                } else {
                    redirect(action: "verifyRequest", params: params)
                }
            } else {
                redirect(action: "createInbound", params: params)
            }
        }
        else {
            if (stockMovement.isFromOrder) {
                redirect(action: "createCombinedShipments", params: params)
            }
            redirect(action: "createOutbound", params: params)
        }
    }

    def show = {
        Location currentLocation = Location.get(session?.warehouse?.id)
        // Pull Outbound Stock movement (Requisition based) or Outbound or Inbound Return (Order based)
        def stockMovement = outboundStockMovementService.getStockMovement(params.id)
        // For inbound stockMovement only
        if (!stockMovement) {
            stockMovement =  stockMovementService.getStockMovement(params.id)
        }
        stockMovement.documents = stockMovementService.getDocuments(stockMovement)

        if (stockMovement?.order) {
            render(view: "/returns/show", model: [stockMovement: stockMovement, currentLocation: currentLocation])
        } else {
            render(view: "show", model: [stockMovement: stockMovement, currentLocation: currentLocation])
        }
    }

    def list = {
        render(template: "/common/react", params: params)
    }

    def rollback = {
        Location currentLocation = Location.get(session.warehouse.id)
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        if (stockMovement.isDeleteOrRollbackAuthorized(currentLocation) ||
                (stockMovement.isFromOrder && currentLocation?.supports(ActivityCode.ENABLE_CENTRAL_PURCHASING))) {
            try {
                stockMovementService.rollbackStockMovement(params.id)
                flash.message = "Successfully rolled back stock movement with ID ${params.id}"
            } catch (Exception e) {
                log.error("Unable to rollback stock movement with ID ${params.id}: " + e.message)
                flash.message = "Unable to rollback stock movement with ID ${params.id}: " + e.message
            }
        } else {
            flash.error = "You are not able to rollback shipment from your location."
        }

        redirect(action: "show", id: params.id)
    }

    def synchronizeDialog = {
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        boolean isAllowed = stockMovementService.isSynchronizationAuthorized(stockMovement)

        def data = stockMovement?.requisition?.picklist?.picklistItems.collect { PicklistItem picklistItem ->
            def expirationDate = picklistItem?.inventoryItem?.expirationDate ?
                    Constants.EXPIRATION_DATE_FORMATTER.format(picklistItem?.inventoryItem?.expirationDate) : null
            return [
                    productCode: picklistItem?.requisitionItem?.product?.productCode,
                    productName: picklistItem?.requisitionItem?.product?.name,
                    binLocation: picklistItem?.binLocation?.name,
                    lotNumber: picklistItem?.inventoryItem?.lotNumber,
                    expirationDate: expirationDate,
                    status: picklistItem?.requisitionItem?.status,
                    requested: picklistItem?.requisitionItem?.quantity,
                    picked: picklistItem?.quantity,
                    pickReasonCode: picklistItem?.reasonCode,
                    editReasonCode: picklistItem?.requisitionItem?.cancelReasonCode
            ]
        }

        render(template: "synchronizeDialog", model: [stockMovement: stockMovement, data: data, isAllowed:isAllowed])
    }

    def synchronize = {
        log.info "params " + params
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        Date dateShipped = params.dateShipped as Date
        if (stockMovementService.isSynchronizationAuthorized(stockMovement)) {
            try {
                stockMovementService.synchronizeStockMovement(params.id, dateShipped)
                flash.message = "Successfully synchronized stock movement with ID ${params.id}"
            } catch (Exception e) {
                log.error("Unable to synchronize stock movement with ID ${params.id}: " + e.message, e)
                flash.message = "Unable to synchronize stock movement with ID ${params.id}: " + e.message
            }
        } else {
            flash.error = "You are not authorized to synchronize this stock movement."
        }

        redirect(action: "show", id: params.id)
    }

    def remove = {
        Location currentLocation = Location.get(session.warehouse.id)
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        String[] urlParts = request.request.requestURI.split("/")
        boolean isRequestedUrlForStockRequest = urlParts[2] == "stockRequest"
        // Check if URL is /stockRequest and if the stockMovement we are trying to delete is a request
        // OR check if url is /stockMovement and the stockMovement we are trying to delete is not request to prevent user from trying to delete request using /stockMovement URL
        if ((isRequestedUrlForStockRequest && !stockMovement.electronicType) ||
            (!isRequestedUrlForStockRequest && stockMovement.electronicType)) {
                throw new IllegalAccessException("You can't delete the stock movement: ${stockMovement.name} using this URL")
        }
        if (stockMovement.isDeleteOrRollbackAuthorized(currentLocation)) {
            if (stockMovement?.shipment?.currentStatus == ShipmentStatusCode.PENDING || !stockMovement?.shipment?.currentStatus) {
                try {
                    stockMovementService.deleteStockMovement(params.id)
                    flash.message = g.message(
                            code: 'react.stockMovement.deleted.success.message.label',
                            default: 'Stock Movement has been deleted successfully',
                    )
                } catch (Exception e) {
                    log.error("Unable to delete stock movement with ID ${params.id}: " + e.message, e)
                    flash.message = "${g.message(code: 'stockMovement.delete.error.message', default: 'The Stock Movement could not be deleted')}"
                    redirect(action: "show", id: params.id)
                    return
                }
            } else {
                flash.message = "You cannot delete a shipment with status ${stockMovement?.shipment?.currentStatus}"
                redirect(action: "show", id: params.id)
                return
            }
        }
        else {
            flash.message = "You are not able to delete stock movement from your location."
            if (params.show) {
                redirect(action: "show", id: params.id)
                return
            }
        }
        if (!currentLocation.supports(ActivityCode.MANAGE_INVENTORY) && currentLocation.supports(ActivityCode.SUBMIT_REQUEST)) {
            redirect(uri: "/dashboard")
            return
        }

        // We need to set the correct parameter so stock movement list is displayed properly
        params.direction = (currentLocation == stockMovement.origin) ? StockMovementDirection.OUTBOUND :
                (currentLocation == stockMovement.destination) ? StockMovementDirection.INBOUND : "ALL"


        redirect(action: "list", params: params << ["flash": flash as JSON])
    }

    def requisition = {
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        render(template: "requisition", model: [stockMovement: stockMovement])

    }

    def documents = {
        def stockMovement = getStockMovement(params.id)
        stockMovement.documents = stockMovementService.getDocuments(stockMovement)
        render(template: "documents", model: [stockMovement: stockMovement])
    }

    def packingList = {
        def stockMovement = getStockMovement(params.id)
        render(template: "packingList", model: [stockMovement: stockMovement])
    }

    def receipts = {
        def stockMovement = getStockMovement(params.id)
        def receiptItems = stockMovementService.getStockMovementReceiptItems(stockMovement)
        render(template: "receipts", model: [receiptItems: receiptItems])
    }

    // Used by SM show page 'tabs' actions - packing list, documents and receipts
    def getStockMovement(String stockMovementId) {
        def stockMovement
        // Pull stock movement in "old fashion" way to bump up performance a bit (instead of getting OutboundStockMovement) for Non-Returns
        def order = Order.get(stockMovementId)
        if (order) {
            stockMovement = outboundStockMovementService.getStockMovement(stockMovementId)
        } else {
            stockMovement = stockMovementService.getStockMovement(stockMovementId)
        }

        return stockMovement
    }

    def uploadDocument = { DocumentCommand command ->
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)

        Shipment shipment = stockMovement.shipment
        Document document = new Document()
        document.fileContents = command.fileContents.bytes
        document.contentType = command.fileContents.fileItem.contentType
        document.name = command.fileContents.fileItem.name
        document.filename = command.fileContents.fileItem.name
        document.documentType = DocumentType.get(Constants.DEFAULT_DOCUMENT_TYPE_ID)

        shipment.addToDocuments(document)
        shipment.save()

        render([data: "Document was uploaded successfully"] as JSON)
    }


    def uploadDocuments = { BulkDocumentCommand command ->
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        Shipment shipment = stockMovement.shipment

        command.filesContents.each { fileContent ->
            Document document = new Document()
            document.fileContents = fileContent.bytes
            document.contentType = fileContent.fileItem.contentType
            document.name = fileContent.fileItem.name
            document.filename = fileContent.fileItem.name
            document.documentType = DocumentType.get(Constants.DEFAULT_DOCUMENT_TYPE_ID)

            shipment.addToDocuments(document)
        }
        shipment.save()

        render([data: "Documents were uploaded successfully"] as JSON)
    }

    def addDocument = {
        log.info params
        def stockMovement = outboundStockMovementService.getStockMovement(params.id)
        if (!stockMovement) {
            stockMovement =  stockMovementService.getStockMovement(params.id)
        }

        Shipment shipmentInstance = stockMovement.shipment
        def documentInstance = Document.get(params?.document?.id)
        if (!documentInstance) {
            documentInstance = new Document()
        }
        if (!shipmentInstance) {
            flash.message = "${warehouse.message(code: 'default.not.found.message', args: [warehouse.message(code: 'shipment.label', default: 'Shipment'), params.id])}"
            redirect(action: "list")
        }
        render(view: "addDocument", model: [shipmentInstance: shipmentInstance, documentInstance: documentInstance, stockMovementInstance: stockMovement])
    }

    def exportCsv = {
        StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
        List lineItems = stockMovementService.buildStockMovementItemList(stockMovement)
        String csv = dataService.generateCsv(lineItems)
        response.setHeader("Content-disposition", "attachment; filename=\"StockMovementItems-${params.id}.csv\"")
        render(contentType: "text/csv", text: csv.toString(), encoding: "UTF-8")
    }


    def importCsv = { ImportDataCommand command ->

        try {
            StockMovement stockMovement = stockMovementService.getStockMovement(params.id)
            Location currentLocation = Location.get(session?.warehouse?.id)

            def importFile = command.importFile
            if (importFile.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty")
            }

            if (importFile.fileItem.contentType != "text/csv") {
                throw new IllegalArgumentException("File must be in CSV format")
            }


            String csv = new String(importFile.bytes)
            char separatorChar = CSVUtils.getSeparator(csv, StockMovement.buildCsvRow()?.keySet()?.size())
            def settings = [separatorChar: separatorChar, skipLines: 1]
            Integer sortOrder = 0
            csv.toCsvReader(settings).eachLine { tokens ->
                Boolean validateLotAndExpiry = stockMovement.getStockMovementDirection(currentLocation) != StockMovementDirection.OUTBOUND && !stockMovement.electronicType
                StockMovementItem stockMovementItem = StockMovementItem.createFromTokens(tokens, validateLotAndExpiry)
                stockMovementItem.stockMovement = stockMovement
                stockMovementItem.sortOrder = sortOrder
                stockMovement.lineItems.add(stockMovementItem)
                sortOrder += 100
            }
            stockMovementService.updateItems(stockMovement)

        } catch (Exception e) {
            // FIXME The global error handler does not return JSON for multipart uploads
            log.warn("Error occurred while importing CSV: " + e.message, e)
            response.status = 500
            render([errorCode: 500, errorMessage: e?.message ?: "An unknown error occurred during import"] as JSON)
            return
        }

        render([data: "Data will be imported successfully"] as JSON)
    }

    // TODO: Remove after implementing inbound sm list on the react side
    def exportItems = {
        def shipmentItems = []
        def shipments = shipmentService.getShipmentsByDestination(session.warehouse)

        shipments.findAll {
            it.currentStatus == ShipmentStatusCode.SHIPPED || it.currentStatus == ShipmentStatusCode.PARTIALLY_RECEIVED
        }.each { shipment ->
            shipment.shipmentItems.findAll { it.quantityRemaining > 0 }.groupBy {
                it.product
            }.each { product, value ->
                shipmentItems << [
                        productCode         : product.productCode,
                        productName         : product.name,
                        quantity            : value.sum { it.quantityRemaining },
                        expectedShippingDate: formatDate(date: shipment.expectedShippingDate, format: "dd-MMM-yy"),
                        expectedDeliveryDate: formatDate(date: shipment.expectedDeliveryDate, format: "dd-MMM-yy"),
                        shipmentNumber      : shipment.shipmentNumber,
                        shipmentName        : shipment.name,
                        origin              : shipment.origin,
                        destination         : shipment.destination,
                ]
            }
        }


        if (shipmentItems) {
            def date = new Date()
            def sw = new StringWriter()

            def csv = new CSVWriter(sw, {
                "Code" { it.productCode }
                "Product Name" { it.productName }
                "Quantity Incoming" { it.quantity }
                "Expected Shipping Date" { it.expectedShippingDate }
                "Expected Delivery Date" { it.expectedDeliveryDate }
                "Shipment Number" { it.shipmentNumber }
                "Shipment Name" { it.shipmentName }
                "Origin" { it.origin }
                "Destination" { it.destination }
            })

            shipmentItems.each { shipmentItem ->
                csv << [
                        productCode         : shipmentItem.productCode,
                        productName         : shipmentItem.productName,
                        quantity            : shipmentItem.quantity,
                        expectedShippingDate: shipmentItem.expectedShippingDate,
                        expectedDeliveryDate: shipmentItem.expectedDeliveryDate,
                        shipmentNumber      : shipmentItem.shipmentNumber,
                        shipmentName        : shipmentItem.shipmentName,
                        origin              : shipmentItem.origin,
                        destination         : shipmentItem.destination,
                ]
            }
            response.contentType = "text/csv"
            response.setHeader("Content-disposition", "attachment; filename=\"Items shipped not received_${session.warehouse.name}_${date.format("yyyyMMdd-hhmmss")}.csv\"")
            render(contentType: "text/csv", text: csv.writer.toString())
            return
        } else {
            render(text: 'No shipments found', status: 404)
        }
    }

    // TODO: Remove after implementing outbound sm list on the react side
    def exportPendingRequisitionItems = {
        Location currentLocation = Location.get(session?.warehouse?.id)

        def pendingRequisitionItems = stockMovementService.getPendingRequisitionItems(currentLocation)

        def sw = new StringWriter()
        def csv = new CSVWriter(sw, {
            "Shipment Number" { it.shipmentNumber }
            "Description" { it.description }
            "Destination" { it.destination }
            "Status" { it.status }
            "Product Code" { it.productCode }
            "Product" { it.productName }
            "Qty Picked" { it.quantityPicked }
        })
        pendingRequisitionItems.each { requisitionItem ->
            def quantityPicked = requisitionItem?.totalQuantityPicked()
            if (quantityPicked) {
                csv << [
                        shipmentNumber  : requisitionItem?.requisition?.requestNumber,
                        description     : requisitionItem?.requisition?.description ?: '',
                        destination     : requisitionItem?.requisition?.destination,
                        status          : requisitionItem?.requisition?.status,
                        productCode     : requisitionItem?.product?.productCode,
                        productName     : requisitionItem?.product?.name,
                        quantityPicked  : quantityPicked,
                ]
            }
        }

        response.setHeader("Content-disposition", "attachment; filename=\"PendingShipmentItems-${new Date().format("yyyyMMdd-hhmmss")}.csv\"")
        render(contentType: "text/csv", text: sw.toString(), encoding: "UTF-8")

    }

}

