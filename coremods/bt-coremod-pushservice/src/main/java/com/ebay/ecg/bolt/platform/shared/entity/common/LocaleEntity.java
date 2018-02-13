package com.ebay.ecg.bolt.platform.shared.entity.common;

import java.util.Locale;

import org.springframework.util.Assert;

import com.ebay.ecg.bolt.platform.shared.util.LocaleUtils;

public final class LocaleEntity {
	private final Locale locale;

	private final String localeString; // internal optimization, canonical java locale string
	
	public LocaleEntity(String localeString) {
		this(LocaleUtils.parseLocale(localeString));
	}

	public LocaleEntity(Locale locale) {
		Assert.notNull(locale);

		this.locale = locale;

		LocaleUtils.validateISOCountryCode(this.locale.getCountry());
		LocaleUtils.validateISOLanguageCode(this.locale.getLanguage());

		this.localeString = this.locale.toString();
	}
	
	public Locale getLocale() {
		return locale;
	}

	@Override
	public int hashCode() {
		return locale.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LocaleEntity other = (LocaleEntity) obj;
		return localeString.equals(other.localeString);
	}

	@Override
	public String toString() {
		return localeString;
	}

    // for deserialization.
    public LocaleEntity() {
        locale = null;
        localeString = null;
    }
}