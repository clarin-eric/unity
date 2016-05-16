/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;


/**
 * This class is useful when creating a new Identity.
 * 
 * @author K. Benedyczak
 */
public class IdentityParam extends IdentityTaV implements VerifiableElement
{
	private String translationProfile;
	private String remoteIdp;
	private ConfirmationInfo confirmationInfo;
	private JsonNode metadata;
	
	public IdentityParam()
	{
	}

	public IdentityParam(String type, String value) 
	{
		super(type, value);
		this.confirmationInfo = new ConfirmationInfo();
	}

	public IdentityParam(String type, String value, String remoteIdp, String translationProfile) 
	{
		this(type, value);
		this.translationProfile = translationProfile;
		this.remoteIdp = remoteIdp;
	}
	
	@JsonCreator
	public IdentityParam(ObjectNode src)
	{
		fromJson(src);
	}
	
	public boolean isLocal()
	{
		return remoteIdp == null;
	}
	
	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public String getRemoteIdp()
	{
		return remoteIdp;
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public void setRemoteIdp(String remoteIdp)
	{
		this.remoteIdp = remoteIdp;
	}

	public JsonNode getMetadata()
	{
		return metadata;
	}

	public void setMetadata(JsonNode metadata)
	{
		this.metadata = metadata;
	}

	/**
	 * Sets confirmation status of the identity. For identity types which does not support confirmations
	 * this setting is ignored. For other the confirmation status is handled automatically in the most cases. 
	 */
	@Override
	public void setConfirmationInfo(ConfirmationInfo confirmationData)
	{
		this.confirmationInfo = confirmationData;
	}

	@Override
	public ConfirmationInfo getConfirmationInfo()
	{
		return confirmationInfo;
	}

	@Override
	public boolean isConfirmed()
	{
		return confirmationInfo != null && confirmationInfo.isConfirmed();
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		return super.toJson();
	}

	@Override
	public ObjectNode toJsonBase()
	{
		ObjectNode main = super.toJsonBase();
		if (getTranslationProfile() != null)
			main.put("translationProfile", getTranslationProfile());
		if (getRemoteIdp() != null)
			main.put("remoteIdp", getRemoteIdp());
		if (getConfirmationInfo() != null)
			main.put("confirmationInfo", getConfirmationInfo().getSerializedConfiguration());
		if (getMetadata() != null)
			main.set("metadata", getMetadata());
		return main;
	}
	
	@Override
	public void fromJsonBase(ObjectNode main)
	{
		super.fromJsonBase(main);
		if (main.has("translationProfile"))
			setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("remoteIdp"))
			setRemoteIdp(main.get("remoteIdp").asText());
		if (main.has("confirmationInfo"))
		{
			ConfirmationInfo conData = new ConfirmationInfo();
			conData.setSerializedConfiguration(main.get("confirmationInfo").asText());
			setConfirmationInfo(conData);
		}
		if (main.has("metadata"))
			setMetadata(main.get("metadata"));
	}

	
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((remoteIdp == null) ? 0 : remoteIdp.hashCode());
		result = prime
				* result
				+ ((translationProfile == null) ? 0 : translationProfile.hashCode());
		result = prime
				* result
				+ ((confirmationInfo == null) ? 0 : confirmationInfo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentityParam other = (IdentityParam) obj;
		if (remoteIdp == null)
		{
			if (other.remoteIdp != null)
				return false;
		} else if (!remoteIdp.equals(other.remoteIdp))
			return false;
		if (translationProfile == null)
		{
			if (other.translationProfile != null)
				return false;
		} else if (!translationProfile.equals(other.translationProfile))
			return false;
		if (confirmationInfo == null)
		{
			if (other.confirmationInfo!= null)
				return false;
		} else if (!confirmationInfo.equals(other.confirmationInfo))
			return false;
		return true;
	}
}