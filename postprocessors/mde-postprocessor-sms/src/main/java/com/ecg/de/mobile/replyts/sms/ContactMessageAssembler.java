package com.ecg.de.mobile.replyts.sms;

import java.util.Date;
import java.util.Map;

public class ContactMessageAssembler {

	private static final String CUSTOMER_ID_PARAM = "X-Cust-Customer_Id";
	private static final String PUBLISHER_PARAM = "X-Cust-Publisher";
	private static final String SELLER_TYPE_PARAM = "X-Cust-Seller_Type";
	private static final String BUYER_TYPE_PARAM = "X-Cust-Buyer_Type";
	private static final String BUYER_SITE_ID_PARAM = "X-Cust-Buyer_Site_Id";
	private static final String SELLER_SITE_ID_PARAM = "X-Cust-Seller_Site_Id";
	private static final String PHONE_NUMBER_COUNTRY_CODE_PARAM = "X-Cust-Phone_Number_Country_Code";
	private static final String PHONE_NUMBER_DISPLAY_NUMBER_PARAM = "X-Cust-Phone_Number_Display_Number";
	private static final String IP_ADDRESS_V4V6_PARAM = "X-Cust-Ip_Address_V4V6";

	private static final String CUSTOMER_NUMBER_PARAM = "X-Cust-Customer_Number";
	private static final String CURRENCY_PARAM = "X-Cust-Currency";
	private static final String DISPLAY_NAME_PARAM = "X-Cust-Display_Name";
	private static final String INTERNAL_NUMBER_PARAM = "X-Cust-Internal_Number";
	private static final String PRICE_IN_MAIN_UNIT_PARAM = "X-Cust-Price_In_Main_Unit";
	private static final String SELLER_LOCALE_PARAM = "X-Cust-Seller_Locale";
	private static final String SMS_PHONE_NUMBER = "X-Cust-Sms_Phone_Number";
	private static final String MAKE_NAME_PARAM = "X-Cust-Make_Name";
	private static final String MODEL_DESCRIPTION_PARAM = "X-Cust-Model_Description";
    
	public static ContactMessage assemble(String buyerMailAddress, Date sentDate, Map<String, String> headers){
		return new ContactMessage.Builder()
		.buyerMailAddress(buyerMailAddress)
		.buyerPhoneNumber(phoneNumber(headers))
		.buyerSiteId(headers.get(BUYER_SITE_ID_PARAM))
		.buyerType(headers.get(BUYER_TYPE_PARAM))
		.currency(headers.get(CURRENCY_PARAM))
		.customerNumber(toLong(headers.get(CUSTOMER_NUMBER_PARAM)))
		.customerId(toLong(headers.get(CUSTOMER_ID_PARAM)))
		.displayName(headers.get(DISPLAY_NAME_PARAM))
		.internalNumber(headers.get(INTERNAL_NUMBER_PARAM))
		.ipAddressV4V6(headers.get(IP_ADDRESS_V4V6_PARAM))
		.makeName(headers.get(MAKE_NAME_PARAM))
		.messageCreatedTime(sentDate)
		.modelDescription(headers.get(MODEL_DESCRIPTION_PARAM))
		.priceInMainUnit(toInteger(headers.get(PRICE_IN_MAIN_UNIT_PARAM)))
		.sellerLocale(headers.get(SELLER_LOCALE_PARAM))
		.sellerType(headers.get(SELLER_TYPE_PARAM))
		.sellerSiteId(headers.get(SELLER_SITE_ID_PARAM))
		.publisher(headers.get(PUBLISHER_PARAM))
		.smsPhoneNumber(headers.get(SMS_PHONE_NUMBER))
		.get();

	}
	
	private static long toLong(String value){
		try{
		return Long.valueOf(value);
		} catch (Exception e){
			
		}
		return 0L;
	}
	
	private static int toInteger(String value){
		try{
		return Integer.valueOf(value);
		} catch (Exception e){
			
		}
		return 0;
	}
	
    private static PhoneNumber phoneNumber(Map<String, String> headers){
        String countryCode = headers.get(PHONE_NUMBER_COUNTRY_CODE_PARAM);
        String displayNumber = headers.get(PHONE_NUMBER_DISPLAY_NUMBER_PARAM);
        if(countryCode == null || displayNumber == null) {
            return null;
        }
        return new PhoneNumber(Integer.valueOf(countryCode),displayNumber);

    }
}
