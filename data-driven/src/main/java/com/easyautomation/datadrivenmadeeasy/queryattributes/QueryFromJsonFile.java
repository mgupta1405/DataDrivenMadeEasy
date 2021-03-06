package com.easyautomation.datadrivenmadeeasy.queryattributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.easyautomation.datadrivenmadeeasy.exceptions.InvalidQueryException;
import com.easyautomation.datadrivenmadeeasy.json.AccessJson;

public class QueryFromJsonFile {
	
	Map<Query, String> queryMatcher;
	AccessJson accessJson;
	
	enum Query {
		SELECT_OBJECT,
		SELECT_ARRAY_KEY,
		SELECT_OBJECT_FIRST_ARRAY,
		SELECT_OBJECT_LAST_ARRAY,
		SELECT_OBJECT_ALL_ARRAY,
		SELECT_OBJECT_FIRST_ARRAY_CONDITION,
		SELECT_OBJECT_LAST_ARRAY_CONDITION,
		SELECT_OBJECT_ALL_ARRAY_CONDITION,
		SELECT_VALUE_FIRST_ARRAY,
		SELECT_VALUE_LAST_ARRAY,
		SELECT_VALUE_ALL_ARRAY,
		SELECT_VALUE_KEY,
		ADD_KEYVALUE
	}
	
	public QueryFromJsonFile() {
		accessJson = new AccessJson();
		queryMatcher = new HashMap<Query, String>();
		queryMatcher.put(Query.SELECT_OBJECT, "SELECT OBJECT");
		queryMatcher.put(Query.SELECT_ARRAY_KEY, "SELECT ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_OBJECT_FIRST_ARRAY, "SELECT OBJECT FIRST FROM ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_OBJECT_FIRST_ARRAY_CONDITION, "SELECT OBJECT FIRST FROM ARRAY WITH (.*) CONDITION (.*)");
		queryMatcher.put(Query.SELECT_OBJECT_LAST_ARRAY, "SELECT OBJECT LAST FROM ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_OBJECT_LAST_ARRAY_CONDITION, "SELECT OBJECT LAST FROM ARRAY WITH (.*) CONDITION (.*)");
		queryMatcher.put(Query.SELECT_OBJECT_ALL_ARRAY, "SELECT OBJECT ALL FROM ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_OBJECT_ALL_ARRAY_CONDITION, "SELECT OBJECT ALL FROM ARRAY WITH (.*) CONDITION (.*)");
		queryMatcher.put(Query.SELECT_VALUE_FIRST_ARRAY, "SELECT VALUE FIRST FROM ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_VALUE_LAST_ARRAY, "SELECT VALUE LAST FROM ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_VALUE_ALL_ARRAY, "SELECT VALUE ALL FROM ARRAY WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.SELECT_VALUE_KEY, "SELECT VALUE WITH [a-zA-Z0-9]*");
		queryMatcher.put(Query.ADD_KEYVALUE, "ADD VALUE [a-zA-Z0-9]* [a-zA-Z0-9]*");
	}
	
	// Select *
	// Select prop1, prop2 where prop3=value and prop4=value
	// Update prop=value where prop3=value
	// Add prop=value
	// ADD prop value
	
	public Query getQueryType(String query) throws Exception {
		Query queryType = null;
		try {
			Iterator<Entry<Query, String>> it = queryMatcher.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<Query, String> matcher = (Map.Entry<Query, String>) it.next();
				if(query.matches(matcher.getValue())) {
					queryType = matcher.getKey();
					break;
				}
			}
			if(queryType == null) {
				throw new InvalidQueryException();
			}
		}
		catch(InvalidQueryException e) {
			throw new InvalidQueryException("Query " + query + " is not valid");
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
		return queryType;
	}
	
	public Object executeSelectQuery(String query, JSONObject jsonObject) throws Exception {
		Object object = null;
		try {
			Query queryType = getQueryType(query);
			switch(queryType) {
				// SELECT OBJECT
				case SELECT_OBJECT: { 
					object = (Object) jsonObject;
				}
				break;
				// SELECT ARRAY WITH KEY
				case SELECT_ARRAY_KEY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					JSONArray array = accessJson.getJSONArray(jsonObject, key);
					object = (Object) array;
				}
				break;
				// SELECT OBJECT FIRST FROM ARRAY WITH KEY
				case SELECT_OBJECT_FIRST_ARRAY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					List<JSONObject> allObjects = accessJson.getAllObjectsFromJSONArray(jsonObject, key);
					if(allObjects.size() > 0) {
						object = (Object) allObjects.get(0);
					}
				}
				break;
				// SELECT OBJECT FIRST FROM ARRAY WITH KEY CONDITION key1 EQUALS value1 AND key2 CONTAINS value2
				case SELECT_OBJECT_FIRST_ARRAY_CONDITION: {
					String regex = queryMatcher.get(queryType);
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(query);
					if(matcher.matches()) {
						String arrayKey = matcher.group(1);
						String conditions = matcher.group(2);
						String[] conditionList = getConditions(conditions);
						List<JSONObject> filteredObjects = accessJson.getObjectsFromJSONArrayBasedOnCondition(jsonObject, arrayKey, conditionList);
						if(filteredObjects.size() > 0) {
							object = (Object) filteredObjects.get(0);
						}
					}
				}
				break;
				// SELECT OBJECT LAST FROM ARRAY WITH KEY
				case SELECT_OBJECT_LAST_ARRAY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					List<JSONObject> allObjects = accessJson.getAllObjectsFromJSONArray(jsonObject, key);
					if(allObjects.size() > 0) {
						List<JSONObject> obj = new ArrayList<JSONObject>();
						obj.add(allObjects.get(allObjects.size()-1));
						object = (Object) allObjects.get(allObjects.size()-1);
					}
				}
				break;
				// SELECT OBJECT FIRST FROM ARRAY WITH KEY CONDITION key1 EQUALS value1 AND key2 CONTAINS value2
				case SELECT_OBJECT_LAST_ARRAY_CONDITION: {
					String regex = queryMatcher.get(queryType);
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(query);
					if(matcher.matches()) {
						String arrayKey = matcher.group(1);
						String conditions = matcher.group(2);
						String[] conditionList = getConditions(conditions);
						List<JSONObject> filteredObjects = accessJson.getObjectsFromJSONArrayBasedOnCondition(jsonObject, arrayKey, conditionList);
						if(filteredObjects.size() > 0) {
							object = (Object) filteredObjects.get(filteredObjects.size()-1);
						}
					}
				}
				break;
				// SELECT OBJECT ALL FROM ARRAY WITH KEY
				case SELECT_OBJECT_ALL_ARRAY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					List<JSONObject> allObjects = accessJson.getAllObjectsFromJSONArray(jsonObject, key);
					object = (Object) allObjects;
				}
				break;
				// SELECT OBJECT FIRST FROM ARRAY WITH KEY CONDITION key1 EQUALS value1 AND key2 CONTAINS value2
				case SELECT_OBJECT_ALL_ARRAY_CONDITION: {
					String regex = queryMatcher.get(queryType);
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(query);
					if(matcher.matches()) {
						String arrayKey = matcher.group(1);
						String conditions = matcher.group(2);
						String[] conditionList = getConditions(conditions);
						List<JSONObject> filteredObjects = accessJson.getObjectsFromJSONArrayBasedOnCondition(jsonObject, arrayKey, conditionList);
						if(filteredObjects.size() > 0) {
							object = (Object) filteredObjects;
						}
					}
				}
				break;
				// SELECT VALUE FIRST FROM ARRAY WITH KEY
				case SELECT_VALUE_FIRST_ARRAY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					Object[] allValues = accessJson.getAllValuesFromJSONArray(jsonObject, key);
					object = (Object) allValues[0];
				}
				break;
				// SELECT VALUE LAST FROM ARRAY WITH KEY
				case SELECT_VALUE_LAST_ARRAY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					Object[] allValues = accessJson.getAllValuesFromJSONArray(jsonObject, key);
					object = (Object) allValues[allValues.length-1];
				}
				break;
				// SELECT VALUE ALL FROM ARRAY WITH KEY
				case SELECT_VALUE_ALL_ARRAY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					Object[] allValues = accessJson.getAllValuesFromJSONArray(jsonObject, key);
					object = (Object) allValues;
				}
				break;
				// SELECT VALUE WITH KEY
				case SELECT_VALUE_KEY: {
					String[] parts = query.split(" ");
					String key = parts[parts.length-1];
					Object value = accessJson.getPropertyValueFromJSONObject(jsonObject, key);
					object = (Object) value;
				}
				break;
				// ADD VALUE key valueToEnter
				case ADD_KEYVALUE: {
					String[] parts = query.split(" ");
					String key = parts[1];
					String value = parts[2];
					JSONObject json = accessJson.addPropertyToJSONObject(jsonObject, key, value);
					object = (Object) json;
				}
				break;
			}
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
		
		return object;
	}

	public void executeAddQuery(String query, JSONObject jsonObject) {
		
	}
	
	public String[] getConditions(String conditions) throws Exception {
		String[] conditionsList = {};
		try {
			if(conditions.contains(" AND ")) {
				conditionsList = conditions.split(" AND ");
			}
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
		
		return conditionsList;
	}
}