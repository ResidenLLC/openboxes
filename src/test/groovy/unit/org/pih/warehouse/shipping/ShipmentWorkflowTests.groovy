/**
* Copyright (c) 2012 Partners In Health.  All rights reserved.
* The use and distribution terms for this software are covered by the
* Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
* which can be found in the file epl-v10.html at the root of this distribution.
* By using this software in any fashion, you are agreeing to be bound by
* the terms of this license.
* You must not remove this notice, or any other, from this software.
**/ 
package org.pih.warehouse.shipping

import grails.test.*
import org.junit.Ignore
import org.pih.warehouse.shipping.ShipmentType
import org.pih.warehouse.shipping.ShipmentWorkflow

@Ignore
class ShipmentWorkflowTests {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIsExcluded() {
    	ShipmentType shipmentType = new ShipmentType([name:"Some Shipment Type"])
    	ShipmentWorkflow workflow = new ShipmentWorkflow([name        :"Some Shipment Workflow",
                                                          shipmentType:shipmentType])
    	
    	workflow.excludedFields = "shipmentNumber,expectedShipmentDate,expectedArrivalDate"
    		
    	assert workflow.isExcluded("expectedShipmentDate")
    	assert workflow.isExcluded("shipmentNumber")
    	assert workflow.isExcluded("expectedArrivalDate")
    	assert workflow.isExcluded("ExpecTedshipmeNtDate")
    	assert !workflow.isExcluded("name")
    }
}
