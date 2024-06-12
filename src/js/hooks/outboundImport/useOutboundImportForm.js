import { useEffect, useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import moment from 'moment/moment';
import { useForm } from 'react-hook-form';
import { useSelector } from 'react-redux';

import notification from 'components/Layout/notifications/notification';
import NotificationType from 'consts/notificationTypes';
import { DateFormat } from 'consts/timeFormat';
import useOutboundImportValidation from 'hooks/outboundImport/useOutboundImportValidation';
import useTranslate from 'hooks/useTranslate';

// TODO: Remove this before feature is finished
const testRow = {
  product: {
    id: 'someId',
    name: 'Some produc tname',
    productCode: '10002',
  },
  lotNumber: 'TE11',
  expirationDate: '09/16/2027',
  quantityPicked: 2,
  binLocation: {
    id: 'someBinId',
    name: 'CUB1C',
    zone: {
      id: 'zoneId',
      name: 'someZone',
    },
  },
  recipient: {
    id: 'someuserId',
    firstName: 'first',
    lastName: 'last',
    username: 'someusername',
    name: 'first last',
  },
};
// TODO: Remove this before feature is finished
const tableErrors = {
  3: { 'product.productCode': 'product with this product code does not exist', lotNumber: 'lot number doe snot exist' },
  4: { lotNumber: 'lot number doe snot exist', quantityPicked: 'qty cant be this value' },
};
// TODO: Remove this before feature is finished
const otherData = [...Array(250).keys()].map(it => ({
  ...testRow,
  quantityPicked: it,
}));

const useOutboundImportForm = ({ next }) => {
  const translate = useTranslate();
  const { validationSchema } = useOutboundImportValidation();
  const { currentLocation } = useSelector((state) => ({
    currentLocation: state.session.currentLocation,
  }));

  const getDefaultValues = () => ({
    description: undefined,
    origin: {
      id: currentLocation?.id,
      label: `${currentLocation?.name} [${currentLocation?.locationType?.description}]`,
    },
    destination: undefined,
    requestedBy: undefined,
    dateRequested: undefined,
    dateShipped: moment(new Date()).format(DateFormat.MMM_DD_YYYY_HH_MM_SS),
    shipmentType: undefined,
    trackingNumber: undefined,
    comments: undefined,
    expectedDeliveryDate: undefined,
    packingList: undefined,
  });

  const [lineItems, setLineItems] = useState([]);
  const [lineItemErrors, setLineItemErrors] = useState({});

  const {
    control,
    getValues,
    handleSubmit,
    formState: { errors, isValid },
    trigger,
    setValue,
  } = useForm({
    mode: 'onBlur',
    defaultValues: getDefaultValues(),
    resolver: (values, context, options) =>
      zodResolver(validationSchema(values))(values, context, options),
  });

  // TODO: implement data validation request
  const onSubmitStockMovementDetails = (values) => {
    // here distinguish whether the onSubmit happens from detalis step or confirm page.
    // if it happens from details step, send an endpoint to validate the data,
    // if from confirm page - save & validate
    console.log('Sending values for validation', values);
    setLineItems(otherData);
    setLineItemErrors(tableErrors);
    next();
  };

  // TODO: implement confirm import logic
  const onConfirmImport = (values) => {
    // here distinguish whether the onSubmit happens from detalis step or confirm page.
    // if it happens from details step, send an endpoint to validate the data,
    // if from confirm page - save & validate
    console.log('Sending values for saving import', values, lineItems);
    notification(NotificationType.SUCCESS)({
      message: translate('react.outboundImport.form.created.success.label', 'Stock Movement has been created successfully'),
    });
    // TODO: redirect to created stockMovement show page
  };

  useEffect(() => {
    if (currentLocation) {
      setValue('origin', {
        id: currentLocation?.id,
        label: `${currentLocation?.name} [${currentLocation?.locationType?.description}]`,
      });
    }
  }, [currentLocation?.id]);

  return {
    control,
    getValues,
    handleSubmit,
    errors,
    isValid,
    onSubmitStockMovementDetails,
    onConfirmImport,
    trigger,
    lineItemErrors,
    lineItems,
  };
};

export default useOutboundImportForm;
