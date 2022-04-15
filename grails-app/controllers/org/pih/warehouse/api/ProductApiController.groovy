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
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.pih.warehouse.core.Location
import org.pih.warehouse.product.Product
import org.pih.warehouse.product.ProductAssociation
import org.pih.warehouse.product.ProductAssociationTypeCode
import org.pih.warehouse.product.ProductAvailability

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@SecurityRequirement(name = "cookie")
@Tag(
    description = "API for products",
    externalDocs = @ExternalDocumentation(
        description = "wiki",
        url = "https://openboxes.atlassian.net/wiki/spaces/OBW/pages/1291288624/Configure+Products"
    ),
    name = "Product"
)
@Transactional
class ProductApiController extends BaseDomainApiController {

    def productService
    def inventoryService
    def forecastingService
    GrailsApplication grailsApplication
    def productAvailabilityService

    class ProductDemandResponse implements Serializable {
        class ProductDemand implements Serializable {
            Product product
            Location location
            Map demand
        }

        ProductDemand data
    }

    @GET
    @Operation(
        summary = "calculate demand for one product at the currently-selected warehouse",
        description = """\
This entry point reports daily, monthly, and yearly demand, as well as a
calculation of how long current stock will last. It also provides detailed
information on the the requested product and currently-selected warehouse.""",
        parameters = [@Parameter(ref = "product_id_in_path")]
    )
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductDemandResponse)
        ),
        description = "demand information for the requested product and warehouse",
        responseCode = "200"
    )
    @Path("/api/products/{id}/demand")
    @Produces("application/json")
    def demand() {
        def response = new ProductDemandResponse()
        response.data.product = Product.get(params.id)
        response.data.location = Location.get(session.warehouse.id)
        response.data.demand = forecastingService.getDemand(response.data.location, response.data.product)
        render(response as JSON)
    }

    class ProductDemandSummaryResponse implements Serializable {
        class ProductDemandSummary implements Serializable {
            Product product
            Location location
            List demand
        }

        ProductDemandSummary data
    }

    @GET
    @Operation(
        summary = "summarize demand history for one product at the currently-selected warehouse",
        parameters = [@Parameter(ref = "product_id_in_path")]
    )
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductDemandResponse)
        ),
        description = "summary information for the requested product and warehouse",
        responseCode = "200"
    )
    @Path("/api/products/{id}/demandSummary")
    @Produces("application/json")
    def demandSummary() {
        def response = new ProductDemandSummaryResponse()
        response.data.product = Product.get(params.id)
        response.data.location = Location.get(session.warehouse.id)
        response.data.demand = forecastingService.getDemandSummary(response.data.location, response.data.product)
        render(response as JSON)
    }

    class ProductSummaryResponse implements Serializable {
        class IdContainer implements Serializable {
            String id
        }
        class ProductSummary implements Serializable {
            IdContainer product
            IdContainer location
            int quantityOnHand
        }

        ProductSummary data
    }

    @GET
    @Operation(
        summary = "report the current quantity of one product at the currently-selected warehouse",
        parameters = [@Parameter(ref = "product_id_in_path")]
    )
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductSummaryResponse)
        ),
        description = "summary information for the requested product and warehouse",
        responseCode = "200"
    )
    @Path("/api/products/{id}/productSummary")
    @Produces("application/json")
    def productSummary() {
        def product = Product.load(params.id)
        def location = Location.load(session.warehouse.id)
        def quantityOnHand = ProductAvailability.findAllByProductAndLocation(product, location).sum { it.quantityOnHand }
        render([data: [product: [id: product.id], location: [id: location.id], quantityOnHand: quantityOnHand]] as JSON)
    }

    class ProductAvailabilityResponse implements Serializable
    {
        List<ProductAvailability> data
    }

    @GET
    @Operation(
        summary = "get detailed availability for one product at the currently-selected warehouse",
        parameters = [@Parameter(ref = "product_id_in_path")]
    )
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductAvailabilityResponse)),
        description = "detailed availability information for the requested product and warehouse",
        responseCode = "200"
    )
    @Path("/api/products/{id}/productAvailability")
    @Produces("application/json")
    def productAvailability() {
        def product = Product.load(params.id)
        def location = Location.load(session.warehouse.id)
        def data = ProductAvailability.findAllByProductAndLocation(product, location)
        render([data: data] as JSON)
    }

    class ProductListResponse implements Serializable
    {
        List<Product> data
    }

    @GET
    @Operation(
        summary = "list products tracked in OpenBoxes",
        description = """\
## Warning!

Do _not_ use Swagger UI's "Try it out" feature on this entry point!

OpenBoxes tracks a large number of products; the full list can
[make this page unresponsive](https://github.com/swagger-api/swagger-ui/issues/3832).
""",
        operationId = "list_products",
        parameters = [
            @Parameter(
                description = "space or comma-separated list of names to filter by (OR)",
                example = "ibuprofen acetaminophen,paracetamol",
                in = ParameterIn.QUERY,
                name = "name",
                required = false
            )
        ])
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductListResponse)
        ),
        description = "a (possibly very long) list of products tracked in OpenBoxes",
        responseCode = "200"
    )
    @Path("/api/products")
    @Produces("application/json")
    def list() {

        def minLength = grailsApplication.config.openboxes.typeahead.minLength
        def location = Location.get(session.warehouse.id)
        def availableItems
        if (params.name && params.name.size() < minLength) {
            render([data: []])
            return
        }

        String[] terms = params?.name?.split(",| ")?.findAll { it }
        def products = productService.searchProducts(terms, [])
        if(params.availableItems) {
            availableItems = inventoryService.getAvailableBinLocations(location, products).groupBy { it.inventoryItem?.product?.productCode }
            products = []
            availableItems.each { k, v ->
                products += [
                    productCode: k,
                    name: v[0].inventoryItem.product.name,
                    id: v[0].inventoryItem.product.id,
                    product: v[0].inventoryItem.product,
                    quantityAvailable: v.sum { it.quantityAvailable },
                    minExpirationDate: v.findAll { it.inventoryItem.expirationDate != null }.collect {
                        it.inventoryItem?.expirationDate
                    }.min()?.format("MM/dd/yyyy"),
                    color: v[0].inventoryItem.product.color
                ]
            }
        }
        products = products.unique()

        render([data: products] as JSON)
    }

    class ProductAvailableItemResponse implements Serializable
    {
        List<AvailableItem> data
    }

    @GET
    @Operation(
        summary = "retrieve bin locations and quantities for one or more products",
        parameters = [
            @Parameter(ref = "product_id_in_path"),
            @Parameter(
                description = "optionally specify additional product ids. This field may be specified more than once",
                example = "12588",
                explode = Explode.TRUE,
                in = ParameterIn.QUERY,
                name = "product.id"
            ),
            @Parameter(
                description = "optionally specify the id of a warehouse to query",
                example = "8a8a9e96687c94ce0168b86793c81a68",
                in = ParameterIn.QUERY,
                name = "location.id"
            )
        ]
    )
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductAvailableItemResponse)
        ),
        description = "a list of bin locations and quantities for the requested products",
        responseCode = "200"
    )
    @Path("/api/products/{id}/availableItems")
    @Produces("application/json")
    def availableItems() {
        def productIds = params.list("product.id") + params.list("id")
        String locationId = params?.location?.id ?: session?.warehouse?.id
        Location location = Location.get(locationId)
        if (!location || productIds.empty) {
            throw new IllegalArgumentException("Must specify a location and at least one product")
        }

        def products = Product.findAllByIdInListAndActive(productIds, true)
        def availableItems = inventoryService.getAvailableBinLocations(location, products)
        render([data: availableItems] as JSON)
    }


    def availableBins() {
        def productIds = params.list("product.id") + params.list("id")
        String locationId = params?.location?.id ?: session?.warehouse?.id
        Location location = Location.get(locationId)

        if (!location || productIds.empty) {
            throw new IllegalArgumentException("Must specify a location and at least one product")
        }

        def products = Product.findAllByIdInListAndActive(productIds, true)
        def availableBins = inventoryService.getAvailableBinLocations(location, products)
        render([data: availableBins] as JSON)
    }


    def substitutions() {
        params.type = ProductAssociationTypeCode.SUBSTITUTE
        params.resource = "substitutions"
        forward(action: "associatedProducts")
    }

    def associatedProducts() {
        Product product = Product.get(params.id)
        ProductAssociationTypeCode[] types = params.list("type")
        log.debug "Types: " + types
        def productAssociations = ProductAssociation.createCriteria().list {
            eq("product", product)
            'in'("code", types)
        }
        def availableItems = []
        boolean hasEarlierExpiringItems = false
        String locationId = params?.location?.id ?: session?.warehouse?.id
        def location = (locationId) ? Location.get(locationId) : null
        if (location) {
            def products = productAssociations.collect { it.associatedProduct }
            log.debug("Location " + location + " products = " + products)

            availableItems = inventoryService.getAvailableItems(location, product)

            productAssociations = productAssociations.collect { productAssociation ->
                def availableProducts = inventoryService.getAvailableProducts(location, productAssociation.associatedProduct)
                def expirationDate = availableProducts.findAll {
                    it.expirationDate != null
                }.collect {
                    it.expirationDate
                }.min()
                def availableQuantity = availableProducts.collect { it.quantity }.sum()
                return [
                        id               : productAssociation.id,
                        type             : productAssociation?.code?.name(),
                        product          : productAssociation.associatedProduct,
                        conversionFactor : productAssociation.quantity,
                        comments         : productAssociation.comments,
                        minExpirationDate: expirationDate,
                        availableQuantity: availableQuantity
                ]
            }
            Date productExpirationDate = availableItems?.collect {
                it.inventoryItem.expirationDate
            }?.min()
            Date otherExpirationDate = productAssociations?.collect { it.minExpirationDate }?.min()
            hasEarlierExpiringItems = productExpirationDate ? productExpirationDate.after(otherExpirationDate) : false
        }

        // This just renames the collection in the JSON so we can match the API called
        // (i.e. resource name is substitutions for /api/products/:id/substitutions)
        params.resource = params.resource ?: "productAssociations"

        render([
                data:
                        [
                                product                : product,
                                availableItems         : availableItems,
                                hasAssociations        : !productAssociations?.empty,
                                hasEarlierExpiringItems: hasEarlierExpiringItems,
                                "${params.resource}"   : productAssociations
                        ]
        ] as JSON)
    }

    def withCatalogs() {
        Product product = Product.get(params.id)

        render([data: [
                id         : product.id,
                name       : product.name,
                productCode: product.productCode,
                catalogs   : product.getProductCatalogs()?.collect {
                    [
                            id  : it.id,
                            name: it.name
                    ]
                }
        ]] as JSON)
    }

    class ProductAvailabilityAndDemand implements Serializable {
        int monthlyDemand
        int quantityOnHand
    }

    @GET
    @Operation(
        summary = "get the monthly demand for, and current quantity of, one product at the currently-selected warehouse",
        parameters = [
            @Parameter(ref = "product_id_in_path"),
            @Parameter(
                description = "the id of a warehouse to query",
                example = "8a8a9e96687c94ce0168b86793c81a68",
                in = ParameterIn.QUERY,
                name = "locationId",
                required = true
            )
        ]
    )
    @ApiResponse(
        content = @Content(
            schema = @Schema(implementation = ProductAvailabilityAndDemand)
        ),
        description = "the monthly demand for, and current quantity of, the specified product",
        responseCode = "200"
    )
    @Path("/api/products/{id}/productAvailabilityAndDemand")
    @Produces("application/json")
    def productAvailabilityAndDemand() {
        Product product = Product.get(params.id)
        Location location = Location.get(params.locationId)
        def response = new ProductAvailabilityAndDemand()
        response.quantityOnHand = productAvailabilityService.getQuantityOnHand(product, location)
        response.monthlyDemand = forecastingService.getDemand(location, product).monthlyDemand
        render(response as JSON)
    }
}
