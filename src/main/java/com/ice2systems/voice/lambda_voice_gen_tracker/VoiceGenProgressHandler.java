package com.ice2systems.voice.lambda_voice_gen_tracker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ice2systems.email.EmailSender;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class VoiceGenProgressHandler implements RequestHandler<SNSEvent, Object> {

	private final AmazonS3 s3;
	private EmailSender emailNotifier;
	private static String bucketOut = "<voice bucket here>";
	
	public VoiceGenProgressHandler() {
		s3 = AmazonS3ClientBuilder.standard().build();
		emailNotifier = EmailSender.getInstance();
	}
	
	public Object handleRequest(SNSEvent event, Context context) {
		SNS sns = event.getRecords().get(0).getSNS();
		String message = sns.getMessage();
		String subject = sns.getSubject();
		String username = null;
		
		context.getLogger().log(String.format("subj=%s message=%s", subject, message));
		
		try {
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(message);
			
			username = (String) json.get("username");
			String countString = (String) json.get("list");
			List<String> nameList = new LinkedList<String>();

			int i = 1;
			
			for(char ch: countString.toCharArray()) {// textLog #
				int count = Integer.parseInt(String.valueOf(ch));
				for(int j=1;j<=count;j++) { // monolog #
					nameList.add(String.format("%s/%s_%s.pcm", subject, String.valueOf(i),String.valueOf(j)));
				}
				i++;
			}
  		
  		while (!nameList.isEmpty()) {
  			context.getLogger().log(String.format("nameList=%s", nameList.toString()));
  			
  			Thread.sleep(500);
  			
  			Iterator<String> iter = nameList.iterator();
  			
  			while(iter.hasNext()) {
  				String key = iter.next();
  				
  				if( s3.doesObjectExist(bucketOut, key) ) {
  					context.getLogger().log(String.format("exists key=%s", key));
  					iter.remove();
  				}
  			}
  		}
  		
  		emailNotifier.sendVoiceReady(subject, username);
		} catch (Exception e) {
			e.printStackTrace();
			context.getLogger().log(String.format("Error getting subject=%s from message=%s", subject, message));
			emailNotifier.sendFailure(e.getMessage(), username);
		}
		
		return null;
	}

}
