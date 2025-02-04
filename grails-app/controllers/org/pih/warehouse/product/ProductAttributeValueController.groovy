/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.product

import org.pih.warehouse.core.EntityTypeCode


class ProductAttributeValueController {

    static scaffold = ProductAttribute
    def dataService
    def documentService

    def exportProductAttribute() {
        def entityTypeCode = params.entityTypeCode ? params.entityTypeCode as EntityTypeCode : null
        def isEntitySupplier = entityTypeCode == EntityTypeCode.PRODUCT_SUPPLIER
        def productAttributes = ProductAttribute.createCriteria().list {
            if (isEntitySupplier) {
                isNotNull("productSupplier")
            } else {
                isNull("productSupplier")
            }
        }

        def filename = isEntitySupplier ? "productSourceAttribute" : "productAttributes"
        def data = productAttributes ? dataService.transformObjects(productAttributes, isEntitySupplier ? ProductAttribute.SUPPLIER_PROPERTIES : ProductAttribute.PROPERTIES) : [[:]]
        response.contentType = "application/vnd.ms-excel"
        response.setHeader("Content-disposition", "attachment; filename=\"${filename}.xls\"")
        documentService.generateExcel(response.outputStream, data)
        response.outputStream.flush()
    }
}
