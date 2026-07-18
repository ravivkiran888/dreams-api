package com.analysis.runner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.analysis.constants.Constants;
import com.analysis.documents.GrowAccessTokenEntity;


@SpringBootApplication
public class GrowAccessTokenRunner  implements CommandLineRunner{
	
	 @Value("${rungrowaccesskeyinsertor:false}")
	  private boolean rungrowaccesskeyinsertor;


    private final MongoTemplate mongoTemplate;

    public GrowAccessTokenRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

   

    @Override
    public void run(String... args) {

    	if(rungrowaccesskeyinsertor)
    	{
    	
        Date expiresAt = Date.from(
                Instant.now().plus(24, ChronoUnit.HOURS)
        );

        
        String accessToken = "eyJraWQiOiJaTUtjVXciLCJhbGciOiJFUzI1NiJ9.eyJleHAiOjE3ODQ0MjEwMDAsImlhdCI6MTc4NDM0MTc2OCwibmJmIjoxNzg0MzQxNzY4LCJzdWIiOiJ7XCJ0b2tlblJlZklkXCI6XCJhZjU4ZjQ0Mi1iYTQ2LTQ2ZWEtOWJiOC03YTUyNzRiNjRiZjZcIixcInZlbmRvckludGVncmF0aW9uS2V5XCI6XCJlMzFmZjIzYjA4NmI0MDZjODg3NGIyZjZkODQ5NTMxM1wiLFwidXNlckFjY291bnRJZFwiOlwiNzEwMjIzNmUtNWY3Ny00YzA2LTg4YTktYTNmZjA3YjJlOTI1XCIsXCJkZXZpY2VJZFwiOlwiY2U5ZDUwYjUtYTdlMS01MDcyLWI2YzEtZjBmM2QwM2VkNmEwXCIsXCJzZXNzaW9uSWRcIjpcImI5YmZmMTVmLWY5MDgtNDY0Mi05MzY1LWZiNTE2N2Y2MWViOFwiLFwiYWRkaXRpb25hbERhdGFcIjpcIno1NC9NZzltdjE2WXdmb0gvS0EwYkE3UHg0UEpSbFc0U0xUeHlxNU5EbVpSTkczdTlLa2pWZDNoWjU1ZStNZERhWXBOVi9UOUxIRmtQejFFQisybTdRPT1cIixcInJvbGVcIjpcIm9yZGVyLWJhc2ljLGxpdmVfZGF0YS1iYXNpYyxub25fdHJhZGluZy1iYXNpYyxvcmRlcl9yZWFkX29ubHktYmFzaWMsYmFja190ZXN0XCIsXCJzb3VyY2VJcEFkZHJlc3NcIjpcIjI0MDU6MjAxOmM0MGI6MTIzMzoxOTVhOjIyN2M6NDE4Yzo0NjYsMTcyLjcxLjE5OC45NSwzNS4yNDEuMjMuMTIzXCIsXCJ0d29GYUV4cGlyeVRzXCI6MTc4NDQyMTAwMDAwMCxcInZlbmRvck5hbWVcIjpcImdyb3d3QXBpXCJ9IiwiaXNzIjoiYXBleC1hdXRoLXByb2QtYXBwIn0.h0ixJxLFX38UNb6p1Mi6Mw5o0irsLldmqdwj6GSTexlMQY_T1beYRhOx0ZISAylIHiGdJykApXl_tpkkLAPjLw";
        
        GrowAccessTokenEntity token = new GrowAccessTokenEntity(
        		accessToken,
                expiresAt
        );

        // Ensure only one fresh token record exists.
        mongoTemplate.remove(new Query(), GrowAccessTokenEntity.class, Constants.GROW_ACCESS_TOKEN_COLLECTION);

        mongoTemplate.save(token, Constants.GROW_ACCESS_TOKEN_COLLECTION);

        System.out.println("Grow token inserted with 24-hour expiry");
        
    	}
        
    }
}