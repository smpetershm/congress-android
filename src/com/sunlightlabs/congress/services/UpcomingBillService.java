package com.sunlightlabs.congress.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.UpcomingBill;

public class UpcomingBillService {
	
	private static String[] fields = {
		"context", "chamber", "legislative_day", "congress", "source_type",
		"bill_id", "bill", "range"
	};
	
	public static List<UpcomingBill> comingUp(Date day) throws CongressException {
		Map<String,String> params = new HashMap<String,String>();
		
		// require an attached bill
		params.put("bill__exists", "true");
		// soonest first, since this is the future
		params.put("order", "legislative_day__asc");
		
		return upcomingBillsFor(Congress.url("upcoming_bills", fields, params, 1, Congress.MAX_PER_PAGE));
	}
	
	protected static UpcomingBill fromAPI(JSONObject json) throws JSONException, ParseException {
		UpcomingBill upcoming = new UpcomingBill();
		
		if (!json.isNull("context"))
			upcoming.context = json.getString("context");
		
		if (!json.isNull("chamber"))
			upcoming.chamber = json.getString("chamber");
		
		// field can be present but actually null 
		if (!json.isNull("legislative_day"))
			upcoming.legislativeDay = Congress.parseDateOnly(json.getString("legislative_day"));
		
		if (!json.isNull("range"))
			upcoming.range = json.getString("range");
		
		if (!json.isNull("bill_id"))
			upcoming.billId = json.getString("bill_id");
		
		if (!json.isNull("source_type"))
			upcoming.sourceType = json.getString("source_type");
		
		if (!json.isNull("url"))
			upcoming.sourceUrl = json.getString("url");
		
		if (!json.isNull("congress"))
			upcoming.congress = json.getInt("congress");
		
		if (!json.isNull("bill"))
			upcoming.bill = BillService.fromAPI(json.getJSONObject("bill"));
		
		return upcoming;
	}
	
	private static List<String> listFrom(JSONArray array) throws JSONException {
		int length = array.length();
		List<String> list = new ArrayList<String>(length);
		
		for (int i=0; i<length; i++)
			list.add(array.getString(i));
		
		return list;
	}
	
	private static List<UpcomingBill> upcomingBillsFor(String url) throws CongressException {
		List<UpcomingBill> upcomings = new ArrayList<UpcomingBill>();
		try {
			JSONArray results = Congress.resultsFor(url);

			int length = results.length();
			for (int i = 0; i < length; i++)
				upcomings.add(fromAPI(results.getJSONObject(i)));

		} catch (JSONException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		} catch (ParseException e) {
			throw new CongressException(e, "Problem parsing a date in the JSON from " + url);
		}
		
		return upcomings;
	}
}