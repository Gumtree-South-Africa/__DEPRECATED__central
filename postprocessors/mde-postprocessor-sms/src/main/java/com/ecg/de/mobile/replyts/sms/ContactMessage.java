package com.ecg.de.mobile.replyts.sms;
import java.util.Date;


public class ContactMessage {


    private final String buyerMailAddress;

    private final PhoneNumber buyerPhoneNumber;
    private final String buyerSiteId;
    private final String buyerType;

    private final String sellerSiteId;

    private final String sellerType;

    private final String ipAddressV4V6;

    private final Date messageCreatedTime;

    private final long customerNumber;
    private final long customerId;
    private final String smsPhoneNumber;
    private final String sellerLocale;
    
    private final String displayName;
    
    private final int priceInMainUnit;
    
    private final String currency;
    
    private final String internalNumber;
    
    private final String makeName;
    
    private final String modelDescription;
    
    private final String publisher;
    
	private ContactMessage(Builder builder){
		this.buyerMailAddress = builder.buyerMailAddress;
		this.buyerPhoneNumber = builder.buyerPhoneNumber;
		this.buyerType = builder.buyerType;
		this.buyerSiteId = builder.buyerSiteId;
		this.currency = builder.currency;
		this.customerNumber = builder.customerNumber;
		this.customerId = builder.customerId;
		this.displayName = builder.displayName;
		this.internalNumber = builder.internalNumber;
		this.ipAddressV4V6 = builder.ipAddressV4V6;
		this.makeName = builder.makeName;
		this.messageCreatedTime = builder.messageCreatedTime;
		this.modelDescription = builder.modelDescription;
		this.priceInMainUnit = builder.priceInMainUnit;
		this.sellerLocale = builder.sellerLocale;
		this.sellerType = builder.sellerType;
		this.sellerSiteId = builder.sellerSiteId;
		this.smsPhoneNumber = builder.smsPhoneNumber;
		this.publisher = builder.publisher;
	}
	
    public static class Builder {
    	
        private String buyerSiteId;
        private String buyerType;
        
        private String buyerMailAddress;

        private PhoneNumber buyerPhoneNumber;

        private String sellerSiteId;

        private String sellerType;

        private String ipAddressV4V6;

        private Date messageCreatedTime;

        private long customerNumber;
        private long customerId;
        private String smsPhoneNumber;
        private String sellerLocale;
        
        private String displayName;
        
        private int priceInMainUnit;
        
        private String currency;
        
        private String internalNumber;
        
        private String makeName;
        
        private String modelDescription;
        
        private String publisher;
        
        public Builder publisher(String publisher) {
			this.publisher = publisher;
			return this;
		}
        
        public Builder buyerSiteId(String buyerSiteId) {
			this.buyerSiteId = buyerSiteId;
			return this;
		}
        
        public Builder buyerType(String buyerType) {
			this.buyerType = buyerType;
			return this;
		}
        
        public Builder customerId(long customerId) {
			this.customerId = customerId;
			return this;
		}
        
        public Builder sellerSiteId(String sellerSiteId) {
			this.sellerSiteId = sellerSiteId;
			return this;
		}
        
    	public Builder buyerMailAddress(String buyerMailAddress) {
			this.buyerMailAddress = buyerMailAddress;
			return this;
		}


		public Builder buyerPhoneNumber(PhoneNumber buyerPhoneNumber) {
			this.buyerPhoneNumber = buyerPhoneNumber;
			return this;
		}


		public Builder sellerType(String sellerType) {
			this.sellerType = sellerType;
			return this;
		}


		public Builder ipAddressV4V6(String ipAddressV4V6) {
			this.ipAddressV4V6 = ipAddressV4V6;
			return this;
		}


		public Builder messageCreatedTime(Date messageCreatedTime) {
			this.messageCreatedTime = messageCreatedTime;
			return this;
		}


		public Builder customerNumber(long customerNumber) {
			this.customerNumber = customerNumber;
			return this;
		}


		public Builder smsPhoneNumber(String smsPhoneNumber) {
			this.smsPhoneNumber = smsPhoneNumber;
			return this;
		}


		public Builder sellerLocale(String sellerLocale) {
			this.sellerLocale = sellerLocale;
			return this;
		}


		public Builder displayName(String displayName) {
			this.displayName = displayName;
			return this;
		}


		public Builder priceInMainUnit(int priceInMainUnit) {
			this.priceInMainUnit = priceInMainUnit;
			return this;
		}


		public Builder currency(String currency) {
			this.currency = currency;
			return this;
		}


		public Builder internalNumber(String internalNumber) {
			this.internalNumber = internalNumber;
			return this;
		}


		public Builder makeName(String makeName) {
			this.makeName = makeName;
			return this;
		}


		public Builder modelDescription(String modelDescription) {
			this.modelDescription = modelDescription;
			return this;
		}


		public ContactMessage get(){
    		return new ContactMessage(this);
    	}
    }
    
	public String getMakeName() {
		return makeName;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	public String getInternalNumber() {
		return internalNumber;
	}


	public int getPriceInMainUnit() {
		return priceInMainUnit;
	}

	public String getCurrency() {
		return currency;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getSellerLocale() {
		return sellerLocale;
	}

	public String getSmsPhoneNumber() {
		return smsPhoneNumber;
	}

	public long getCustomerNumber() {
		return customerNumber;
	}

	public String getBuyerMailAddress() {
        return buyerMailAddress;
    }

    public PhoneNumber getBuyerPhoneNumber() {
		return buyerPhoneNumber;
	}


    public String getBuyerSiteId() {
		return buyerSiteId;
	}

	public String getBuyerType() {
		return buyerType;
	}

	public String getSellerSiteId() {
		return sellerSiteId;
	}

	public long getCustomerId() {
		return customerId;
	}

	public String getSellerType() {
        return sellerType;
    }

    public String getIpAddressV4V6() {
        return ipAddressV4V6;
    }

    public Date getMessageCreatedTime() {
        return messageCreatedTime;
    }

	public String getPublisher() {
		return publisher;
	}

	@Override
	public String toString() {
		return "ContactMessage [buyerMailAddress=" + buyerMailAddress
				+ ", buyerPhoneNumber=" + buyerPhoneNumber + ", buyerSiteId="
				+ buyerSiteId + ", buyerType=" + buyerType + ", sellerSiteId=" + sellerSiteId + ", sellerType="
				+ sellerType + ", ipAddressV4V6=" + ipAddressV4V6
				+ ", messageCreatedTime=" + messageCreatedTime
				+ ", customerNumber=" + customerNumber + ", customerId="
				+ customerId + ", smsPhoneNumber=" + smsPhoneNumber
				+ ", sellerLocale=" + sellerLocale + ", displayName="
				+ displayName + ", priceInMainUnit=" + priceInMainUnit
				+ ", currency=" + currency + ", internalNumber="
				+ internalNumber + ", makeName=" + makeName
				+ ", modelDescription=" + modelDescription + ", publisher="
				+ publisher + "]";
	}
   
    
}

