/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.base.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.base.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.base.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Updates a JSON dump before it is actually imported.
 * Changes are performed in JSON contents, input stream is reset after the changes are performed.
 * 
 * TODO - InvitationWithCode - 2xtime changed from second to millisecond, apply *1000
 * 
 * @author K. Benedyczak
 */
@Component
public class UpdateFrom1_9_x implements Update
{
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired 
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired 
	private AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry;
	
	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		
		Map<Long, ObjectNode> attributeTypesById = new HashMap<>();
		Map<String, ObjectNode> attributeTypesByName = new HashMap<>();
		
		updateAttributeTypes(attributeTypesById, attributeTypesByName, contents);
		
		updateIdentityTypes(contents);
		
		updateIdentitites(contents);

		updateGroups(contents, attributeTypesById);

		updateMembers(contents);
		
		updateAttributes(contents, attributeTypesByName);

//		JsonUtils.nextExpect(jp, "genericObjects");
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(contents));
	}

	private void updateAttributeTypes(Map<Long, ObjectNode> attributeTypesById, 
			Map<String, ObjectNode> attributeTypesByName, ObjectNode contents)
	{
		ArrayNode attrTypes = (ArrayNode) contents.get("attributeTypes");
		if (attrTypes == null)
			return;
		for (JsonNode node: attrTypes)
		{
			long id = node.get("id").asLong();
			String name = node.get("name").asText();
			attributeTypesById.put(id, (ObjectNode) node);
			attributeTypesByName.put(name, (ObjectNode) node);
		}
	}
	
	private void updateIdentityTypes(ObjectNode contents)
	{
		ArrayNode idTypes = (ArrayNode) contents.get("identityTypes"); 
		if (idTypes == null)
			return;
		for (JsonNode node: idTypes)
		{
			ObjectNode oNode = (ObjectNode) node;
			oNode.put("identityTypeProvider", node.get("name").asText());
		}
	}
	
	private void updateIdentitites(ObjectNode contents)
	{
		ArrayNode ids = (ArrayNode) contents.get("identities");
		if (ids == null)
			return;
		for (JsonNode node: ids)
			setIdentityComparableValue((ObjectNode) node);
	}
	
	private void setIdentityComparableValue(ObjectNode src)
	{
		String type = src.get("typeName").asText();
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(type);
		String comparable;
		try
		{
			comparable = idTypeDef.getComparableValue(src.get("value").asText(), 
					src.get("realm").asText(null),
					src.get("target").asText(null));
		} catch (IllegalIdentityValueException e)
		{
			throw new InternalException("Can't deserialize identity: invalid value [" + 
					src.get("value") +"]", e);
		}
		src.put("typeId", type);
		src.put("comparableValue", comparable);
	}

	private void updateGroups(ObjectNode contents, Map<Long, ObjectNode> attributeTypesById)
	{
		ArrayNode src = (ArrayNode) contents.get("groups");
		if (src == null)
			return;
		Map<Long, String> legacyGroupIds = new HashMap<>();
		for (JsonNode node: src)
		{
			String groupPath = node.get("groupPath").asText();
			long id = node.get("id").asLong();
			legacyGroupIds.put(id, groupPath);
		}
	
		for (JsonNode node: src)
			updateGroup((ObjectNode) node, legacyGroupIds, attributeTypesById);
	}
	
	
	private void updateGroup(ObjectNode src, Map<Long, String> legacyGroupIds, 
			Map<Long, ObjectNode> attributeTypesById)
	{
		String groupPath = src.get("groupPath").asText();
		src.put("path", groupPath);
		long id = src.get("id").asLong();
		legacyGroupIds.put(id, groupPath);
		
		ArrayNode oldStatements = (ArrayNode) src.get("attributeStatements");
		if (oldStatements == null)
			return;
		
		for (JsonNode statementO: oldStatements)
		{
			ObjectNode statement = (ObjectNode) statementO;
			if (statement.has("extraGroup"))
			{
				long extraGroupId = statement.get("extraGroup").asLong();
				statement.put("extraGroupName", legacyGroupIds.get(extraGroupId));
			}
			if (statement.has("fixedAttribute-attributeId"))
			{
				long attrId = statement.get("fixedAttribute-attributeId").asLong();
				long groupId = statement.get("fixedAttribute-attributeGroupId").asLong();
				String values = statement.get("fixedAttribute-attributeValues").asText();
				
				String group = legacyGroupIds.get(groupId);
				ObjectNode atDef = attributeTypesById.get(attrId);
				String attributeName = atDef.get("name").asText();
				String attributeSyntax = atDef.get("valueSyntaxId").asText();

				ObjectNode target = objectMapper.createObjectNode();
				toNewAttribute(values, target, attributeName, group, attributeSyntax);
				statement.set("fixedAttribute", target);
			}
		}
	}

	private void updateMembers(ObjectNode contents)
	{
		ArrayNode members = (ArrayNode) contents.get("groupMembers");
		if (members == null)
			return;

		//TODO
	}

	private void updateAttributes(ObjectNode contents, Map<String, ObjectNode> attributeTypesByName)
	{
		ArrayNode attributes = (ArrayNode) contents.get("attributes");
		if (attributes == null)
			return;
		for (JsonNode node: attributes)
			updateStoredAttribute((ObjectNode) node, attributeTypesByName);
	}
	
	private void updateStoredAttribute(ObjectNode src, Map<String, ObjectNode> attributeTypesByName)
	{
		String attr = src.get("attributeName").asText();
		String group = src.get("groupPath").asText();
		String values = src.get("values").asText();
		ObjectNode atDef = attributeTypesByName.get(attr);
		String attributeSyntax = atDef.get("valueSyntaxId").asText();

		toNewAttribute(values, src, attr, group, attributeSyntax);
		src.put("entity", src.get("entity").asLong());
	}
	
	@SuppressWarnings("unchecked")
	private void toNewAttribute(String oldValues, ObjectNode target, String attributeName, String group,
			String valueSyntax)
	{
		target.put("name", attributeName);
		target.put("groupPath", group);
		target.put("valueSyntax", valueSyntax);
		
		ObjectNode old = JsonUtil.parse(oldValues);
		if (old.has("creationTs"))
			target.put("creationTs", old.get("creationTs").asLong());
		if (old.has("updateTs"))
			target.put("updateTs", old.get("updateTs").asLong());
		if (old.has("translationProfile"))
			target.put("translationProfile", old.get("translationProfile").asText());
		if (old.has("remoteIdp"))
			target.put("remoteIdp", old.get("remoteIdps").asText());
		target.put("direct", true);
		
		
		ArrayNode oldValuesA = old.withArray("values");
		ArrayNode newValuesA = target.withArray("values");
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = attributeSyntaxFactoriesRegistry.
				getByName(valueSyntax).createInstance();
		try
		{
			for (JsonNode node: oldValuesA)
			{
				Object read = convertLegacyValue(node.binaryValue(), valueSyntax);
				newValuesA.add(syntax.convertToString(read));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	private Object convertLegacyValue(byte[] binaryValue, String valueSyntax) throws IOException
	{
		switch (valueSyntax)
		{
		case "string":
		case "enum":
			return new String(binaryValue, StandardCharsets.UTF_8);
		case "floatingPoint":
			ByteBuffer bb = ByteBuffer.wrap(binaryValue);
			return bb.getDouble();
		case "integer":
			bb = ByteBuffer.wrap(binaryValue);
			return bb.getLong();
		case "verifiableEmail":
			JsonNode jsonN = Constants.MAPPER.readTree(new String(binaryValue, StandardCharsets.UTF_8));
			return new VerifiableEmail(jsonN);
		case "jpegImage":
			ByteArrayInputStream bis = new ByteArrayInputStream(binaryValue);
			return ImageIO.read(bis);
		default:
			throw new IllegalStateException("Unknown attribute value type, can't be converted: " 
					+ valueSyntax);
		}
	}
}
