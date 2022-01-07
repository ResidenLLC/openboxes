/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.api

import grails.converters.JSON
import grails.validation.ValidationException
import org.apache.http.HttpStatus
import org.codehaus.groovy.grails.web.json.JSONObject
import org.pih.warehouse.picklist.PicklistItem
import org.pih.warehouse.product.Product
import org.pih.warehouse.core.User

class PicklistItemApiController extends BaseDomainApiController {

    def picklistService
    def productAvailabilityService

    def read = {
        PicklistItem picklistItem = PicklistItem.get(params.id)

        def data = picklistItem.toJson()

        data.quantityOnHand =
            productAvailabilityService.getQuantityOnHand(picklistItem.inventoryItem)

        data.quantityAvailableToPromise =
                productAvailabilityService.getQuantityAvailableToPromise(picklistItem.requisitionItem.requisition.origin,
                        picklistItem.binLocation,
                        picklistItem.inventoryItem)

        render ([data: data] as JSON)
    }


    def update = {
        JSONObject jsonObject = request.JSON
        log.info "save " + jsonObject
        String id = params.id
        String productId = jsonObject["product.id"]
        String productCode = jsonObject.productCode
        BigDecimal quantityPicked = new BigDecimal(jsonObject.quantityPicked)

        picklistService.updatePicklistItem(id, productId, productCode, quantityPicked)

        render HttpStatus.SC_OK
    }


}
