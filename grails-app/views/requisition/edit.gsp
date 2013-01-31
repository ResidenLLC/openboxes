<%@ page
	import="grails.converters.JSON; org.pih.warehouse.core.RoleType"%>
<%@ page import="org.pih.warehouse.requisition.RequisitionType"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta name="layout" content="custom" />
<g:set var="entityName" value="${warehouse.message(code: 'requisition.label', default: 'Requisition')}" />
<title><warehouse:message code="${requisition?.id ? 'default.edit.label' : 'default.create.label'}" args="[entityName]" /></title>
<content tag="pageTitle"> <warehouse:message code="${requisition?.id ? 'default.edit.label' : 'default.create.label'}" args="[entityName]" /></content>
<script src="${createLinkTo(dir:'js/knockout/', file:'knockout-2.2.0.js')}" type="text/javascript"></script>
<script src="${createLinkTo(dir:'js/', file:'knockout_binding.js')}" type="text/javascript"></script>
<script src="${createLinkTo(dir:'js/', file:'requisition.js')}" type="text/javascript"></script>
</head>
<body>

	<g:if test="${flash.message}">
		<div class="message">${flash.message}</div>
	</g:if>
	<g:hasErrors bean="${requisition}">
		<div class="errors">
			<g:renderErrors bean="${requisition}" as="list" />
		</div>
	</g:hasErrors>	

	<g:form name="requisitionForm" method="post" action="save">
		<div class="dialog box  ui-validation">
			<div id="requisition-header">
				<div class="title" data-bind="html: requisition.name"></div>
				<div class="time-stamp fade"
					data-bind="html:requisition.lastUpdated"></div>
				<div class="status fade" data-bind="html: requisition.status"></div>
			</div>
			<table id="requisition-body">
				<tr class="prop">
					<td class="name"><label for="origin.id"> <g:if
								test="${requisition.isWardRequisition()}">
								<warehouse:message code="requisition.requestingWard.label" />
							</g:if> <g:else>
								<warehouse:message code="requisition.requestingDepot.label" />
							</g:else>
					</label></td>
					<td class="value ${hasErrors(bean: requisition, field: 'origin', 'errors')}"><g:select name="origin.id"
							from="${locations}" id="depot"
							data-bind="value: requisition.originId" 
							optionKey="id" optionValue="name" class='required' value=""
							noSelection="['null':'']" /></td>
				</tr>
				<g:if test="${requisition.isDepotRequisition()}">
					<tr class="prop">
						<td class="name"><label><warehouse:message
									code="requisition.program.label" /></label></td>
						<td class="value"><input id="recipientProgram"
							name="recipientProgram" class="autocomplete text" size="60"
							placeholder="${warehouse.message(code:'requisition.program.label')}"
							data-bind="autocomplete: {source: '${request.contextPath }/json/findPrograms'}, value: requisition.recipientProgram" />

						</td>
					</tr>
				</g:if>
				<tr class="prop">
					<td class="name"><label><warehouse:message
								code="requisition.requestedBy.label" /></label></td>
					<td class="value"><input name="requestedById"
						data-bind="value: requisition.requestedById" type="hidden" /> <input
						id="requestedBy" name="requestedBy"
						class="autocomplete required text" size="60"
						placeholder="${warehouse.message(code:'requisition.requestedBy.label')}"
						data-bind="autocompleteWithId: {source: '${request.contextPath }/json/searchPersonByName'}, value: requisition.requestedByName" />
					</td>
				</tr>
				<tr class="prop">
					<td class="name"><label><warehouse:message
								code="requisition.dateRequested.label" /></label></td>
					<td class="value"><input
						data-bind="value: requisition.dateRequested" type="hidden" /> <input
						type="text" class="required ui_datepicker text"
						max-date="${new Date()}" id="dateRequested"
						data-bind="date_picker:{}" /></td>
				</tr>
				<tr class="prop">
					<td class="name"><label><warehouse:message
								code="requisition.requestedDeliveryDate.label" /></label></td>
					<td class="value"><input
						data-bind="value: requisition.requestedDeliveryDate" type="hidden" />
						<input class="required ui_datepicker text"
						min-date="${new Date().plus(1)}" type="text"
						id="requestedDeliveryDate" data-bind="date_picker:{}" /></td>
				</tr>
				<tr class="prop">

					<td class="name"><label><warehouse:message
								code="requisition.requestItems.label" /></label></td>
					<td class="value">

						<table id="requisition-items" class="ui-validation-items"
							data-bind="visible: requisition.requisitionItems().length">
							<thead>
								<tr class="prop">
									<th class="center">
										${warehouse.message(code: 'requisitionItem.delete.label')}
									</th>
									<th class="list-header">
										${warehouse.message(code: 'requisitionItem.item.label')}
									</th>
									<th class="list-header">
										${warehouse.message(code: 'requisitionItem.quantity.label')}
									</th>
									<g:if test="${requisition.isDepotRequisition()}">
		            %{--<th class="center">--}% %{--${warehouse.message(code: 'requisitionItem.substitutable.label')}?--}%
											%{--
										</th>--}%
		            				<th class="list-header">
											${warehouse.message(code: 'requisitionItem.recipient.label')}
										</th>
									</g:if>
									<th class="list-header">
										${warehouse.message(code: 'requisitionItem.comment.label')}
									</th>
								</tr>
							<thead>
							<tbody data-bind="foreach: requisition.requisitionItems">
								<tr class="requisitionItemsRow">
									<td class="center"><a href='#'
										data-bind='click: $root.requisition.removeItem' tabindex="-1"> <img
											src="/openboxes/images/icons/silk/delete.png"
											alt="Delete item" style="vertical-align: middle">
									</a></td>
									<td class="list-header"><input type="hidden"
										data-bind="value: productId, uniqueName: true" /> <input
										type="text" name="product"
										placeholder="${warehouse.message(code:'requisition.addItem.label')}"
										class="required autocomplete text" size="80"
										data-bind="search_product: {source: '${request.contextPath }/json/searchProduct', id:'searchProduct'+$index()}, value: productName"
										size="50" /></td>
									<td class="list-header"><input name="quantity" type="text"
										class="required number quantity text" size="10"
										data-bind="value: quantity" /></td>
									<g:if test="${requisition.isDepotRequisition()}">
		          %{--<td class="center">--}% 
		          %{--<input type="checkbox" data-bind="checked: substitutable, uniqueName: true">--}%
				  %{--</td>--}%
		          <td class="list-header"><input type="text"
											data-bind="value: recipient, uniqueName: true" /></td>
									</g:if>
									<td class="list-header"><input type="text"
										data-bind="value: comment, uniqueName: true" size="50"
										class="text" /></td>
									
								</tr>
							</tbody>
							<tfoot>
								<tr>
									<td colSpan="6"><input type="button" class="button"
										name="addRequisitionItemRow"
										data-bind='click: requisition.addItem'
										value="${warehouse.message(code:'requisition.addNewItem.label')}" />
									</td>
								</tr>
							</tfoot>
						</table>
					</td>
				</tr>
			</table>
		</div>
		<input type="hidden" data-bind="value: requisition.id" />
		<div class="center">
			<input type="submit" id="save-requisition" class="button"
				value="${warehouse.message(code: 'default.button.submit.label')}" />
			<g:link action="${requisition?.id ? 'show': 'list'}"
				id="${requisition?.id}">
				<input type="button" class="button" id="cancelRequisition" name="cancelRequisition"
					value="${warehouse.message(code: 'default.button.cancel.label')}" />
			</g:link>

		</div>

	</g:form>

	<script type="text/javascript">
    $(function () {

    	// Hack to make the requisition type in the name more pretty (need to internationalize this)
		var requisitionTypes = new Object();
		requisitionTypes['WARD_ADHOC'] = 'Adhoc';
		requisitionTypes['WARD_STOCK'] = 'Stock';
		requisitionTypes['WARD_NON_STOCK'] = 'Non Stock'; 
		requisitionTypes['DEPOT'] = 'Depot'; 
		requisitionTypes['DEPOT_STOCK'] = 'Depot'; 
		requisitionTypes['DEPOT_NON_STOCK'] = 'Depot'; 
		requisitionTypes['DEPOT_TO_DEPOT'] = 'Depot'; 
        
        var requisitionFromServer = ${requisition.toJson() as JSON};
        var requisitionFromLocal = openboxes.requisition.getRequisitionFromLocal(requisitionFromServer.id);
        var requisitionData = openboxes.requisition.Requisition.getNewer(requisitionFromServer, requisitionFromLocal);
        var viewModel = new openboxes.requisition.EditRequisitionViewModel(requisitionData);
        var requisitionId = viewModel.requisition.id();
        viewModel.savedCallback = function(){
            if(!requisitionId) {
                window.location = "${request.contextPath}/requisition/edit/" + viewModel.requisition.id();
            } else {
                window.location = "${request.contextPath}/requisition/show/" + viewModel.requisition.id();
            }
        };
        ko.applyBindings(viewModel);

        $("#requisitionForm").validate({
            submitHandler: viewModel.save,
            rules:  {
                product: { required: true },
                quantity: { required: true, min: 1 },
                requestedBy: { required: true }
            },
            messages: {
                product: { required: "${warehouse.message(code: 'inventoryItem.productNotSupported.message')}" },
                requestedBy: { required: "${warehouse.message(code: 'person.notFound.message')}" }
            }
        });

        if (!viewModel.requisition.name())
            viewModel.requisition.name("" + requisitionTypes[viewModel.requisition.type()] + " " + "${warehouse.message(code: 'requisition.label')}");

        var updateDescription = function () {
            var type = requisitionTypes[viewModel.requisition.type()] + " ";
            var depot = $("select#depot option:selected").text() || "";
            var program = $("#recipientProgram").val() || "";
            var requestedBy = $("#requestedBy").val() || "";
            var dateRequested = $("#dateRequested").val() || "";
            var description = type + "${warehouse.message(code: 'requisition.label', default: 'Requisition')}";
            if(depot) {
                description += " - " + depot;
            }
            if(program != "") {
                description += " - " + program;
            }
            if(requestedBy != "") {
                description += " - " + requestedBy;
            }
            description += " - " + dateRequested;
            viewModel.requisition.name(description);
        };

        $(".value").change(updateDescription);
        setInterval(function () {
            openboxes.requisition.saveRequisitionToLocal(viewModel.requisition);
        }, 3000);

        $("#cancelRequisition").click(function() {
            if(confirm('${warehouse.message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}')) {
                openboxes.requisition.deleteRequisitionFromLocal(requisitionFromServer.id);
                return true;
            }
        });

        $("input.quantity").keyup(function(){
           this.value=this.value.replace(/[^\d]/,'');      
           $(this).trigger("change");//Safari and IE do not fire change event for us!
        });


        


    });
</script>
</body>
</html>
