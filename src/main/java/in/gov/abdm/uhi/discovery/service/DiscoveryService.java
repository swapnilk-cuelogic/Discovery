package in.gov.abdm.uhi.discovery.service;

import java.util.List;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.dhp.sdk.beans.Ack;
import com.dhp.sdk.beans.Error;
import com.dhp.sdk.beans.MessageAck;
import com.dhp.sdk.beans.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.abdm.uhi.discovery.entity.Message;
import in.gov.abdm.uhi.discovery.entity.Subscriber;
import reactor.core.publisher.Mono;

/**
 * @author Deepak Kumar
 *
 */
@Service
public class DiscoveryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryService.class);

	public static Response process(@Valid String req) {

		// Call to lookup
		Message listsubs = lookup(req);
		System.out.println(listsubs.message.toArray());
		/*
		 * ObjectMapper mapper = new ObjectMapper();
		 * 
		 * JsonNode node; try { node = mapper.readTree(req); System.out.println("node|"
		 * + node.get(0)); } catch (JsonMappingException e1) { // TODO Auto-generated
		 * catch block e1.printStackTrace(); } catch (JsonProcessingException e1) { //
		 * TODO Auto-generated catch block e1.printStackTrace(); }
		 * 
		 * // JsonNode subs = node.get(0); // System.out.println(subs.asText());
		 * JsonNode arrNode;
		 */
		try {

			/*
			 * arrNode = mapper.readTree(req).get("subscribers"); System.out.println("size|"
			 * + arrNode.size()); if (arrNode.isArray()) {
			 */
			for (Subscriber subs : listsubs.message) {
				 System.out.println(listsubs.message.size());

				/*
				 * WebClient webClient = WebClient.builder()
				 * .baseUrl("http://localhost:3131/gateway")
				 * .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				 * .build();
				 */

				WebClient webClient = WebClient.create(subs.getUrl());
				// WebClient.create(objNode.get("url").asText());
				webClient.post().uri("/search").body(Mono.just(req), Response.class).retrieve()
						.onStatus(HttpStatus::isError, clientResponse -> {
							clientResponse.bodyToMono(String.class).flatMap(responseBody -> {
								LOGGER.info("Body from within flatMap within onStatus: {}", responseBody);
								return Mono.just(responseBody);
							});
							return Mono.error(new RuntimeException("Resolved!"));
						}).bodyToMono(String.class).subscribe();
				// System.out.println(objNode + "|" + ackResponseMono);
			}
			return generateAck(req);
			// return ackResponseMono;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			generateNack(req, e);
		}

		return generateAck(req);
	}

	public static Response generateAck(String req) {
		Ack ack = new Ack();
		Response resp = new Response();
		MessageAck msgack = new MessageAck();
		Error err = new Error("", "", "", "");
		ack.setStatus("ACK");
		msgack.setAck(ack);
		resp.setMessage(msgack);
		resp.setError(err);

		return resp;
	}

	public static Response generateNack(String req, Exception ex) {
		Ack ack = new Ack();
		Error err = new Error("", "500", ex.getClass().getCanonicalName(), ex.getMessage());
		Response resp = new Response();
		MessageAck msgack = new MessageAck();
		ack.setStatus("NACK");
		msgack.setAck(ack);
		resp.setMessage(msgack);
		resp.setError(err);

		return resp;
	}

	public static String getValueAsString(String name, JsonNode objectNode) {
		String propertyValue = null;
		JsonNode propertyNode = objectNode.get(name);
		if (propertyNode != null && !propertyNode.isNull()) {
			propertyValue = propertyNode.asText();
		}
		return propertyValue;
	}

	static public Message lookup(String req) {

		ObjectMapper objectMapper = new ObjectMapper();
		WebClient webClient = WebClient.create("http://192.168.79.236:3030");
		String message;
		try {

			String lookupRequestString = "{\"country\":\"IND\",\"city\":\"std:080\",\"domain\":\"nic2004:85110\",\"type\":\"BPP\",\"status\":\"SUBSCRIBED\"}";
			Subscriber lookupRequest = objectMapper.readValue(lookupRequestString, Subscriber.class);
			return webClient.post().uri("/api/lookup")
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body(Mono.just(lookupRequest), Message.class).retrieve().bodyToMono(Message.class).block();
		} catch (Exception e) {

			e.printStackTrace();
		}
		return null;

	}
}
