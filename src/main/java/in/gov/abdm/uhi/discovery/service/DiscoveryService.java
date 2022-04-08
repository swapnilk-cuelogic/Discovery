package in.gov.abdm.uhi.discovery.service;

import java.util.List;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.abdm.uhi.discovery.controller.DiscoveryController;
import in.gov.abdm.uhi.discovery.entity.Message;
import in.gov.abdm.uhi.discovery.entity.Subscriber;
import in.gov.abdm.uhi.discovery.service.beans.Ack;
import in.gov.abdm.uhi.discovery.service.beans.MessageAck;
import in.gov.abdm.uhi.discovery.service.beans.Response;
import reactor.core.publisher.Mono;
import in.gov.abdm.uhi.discovery.service.beans.Error;

/**
 * @author Deepak Kumar
 *
 */
@Service
public class DiscoveryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryService.class);

	@Value("${spring.application.registryurl}")
	String registry_url;

	public Response process(@Valid String req) {
		System.out.println("registry_url" + registry_url);
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

	public Response generateAck(String req) {
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

	public  Response generateNack(String req, Exception ex) {
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

	public Message lookup(String req) {

		ObjectMapper objectMapper = new ObjectMapper();
		//String registry_url = "http://192.168.79.236:3030";
		System.out.println("registry_url|" + registry_url);
		//WebClient webClient = WebClient.create(registry_url);
		//String message;
		try {

			String lookupRequestString = "{\"country\":\"IND\",\"city\":\"std:080\",\"domain\":\"nic2004:85110\",\"type\":\"BPP\",\"status\":\"SUBSCRIBED\"}";

			Subscriber lookupRequest = objectMapper.readValue(lookupRequestString, Subscriber.class);
			/*
			 * return webClient.post().uri("/api/lookup") .header(HttpHeaders.CONTENT_TYPE,
			 * MediaType.APPLICATION_JSON_VALUE) .body(Mono.just(lookupRequest),
			 * Message.class).retrieve().bodyToMono(Message.class).block();
			 */

			/*
			 * return webClient.post() .uri("/api/lookup").body(Mono.just(lookupRequest),
			 * Message.class) .retrieve() .bodyToMono(Message.class) .flatMap(s ->
			 * webClient.post() .uri("/api/lookup") .retrieve() .bodyToMono(Message.class) )
			 * .block();
			 */
			RestTemplate template = new RestTemplate();
			Message resp = template.postForObject(registry_url + "/api/lookup", lookupRequest, Message.class);
			LOGGER.info("Company register successfully::" + resp.toString());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return resp;

	}
}
