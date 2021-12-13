package com.microsoft.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.security.token.AccessToken;
import com.sap.cloud.security.token.TokenClaims;
import com.sap.cloud.security.xsuaa.client.ClientCredentials;
import com.sap.cloud.security.xsuaa.client.DefaultOAuth2TokenService;
import com.sap.cloud.security.xsuaa.client.XsuaaDefaultEndpoints;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Servlet implementation class HelloTokenServlet
 */
@WebServlet("/hello-token")

// configure servlet to check against scope "$XSAPPNAME.Display"
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Display" }))
public class HelloTokenServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		AccessToken accessToken = (AccessToken) request.getUserPrincipal();

		response.getWriter().append("Client ID: ")
				.append("" + accessToken.getClaimAsString(accessToken.getClientId()));
		response.getWriter().append("\n");
		response.getWriter().append("Email: ").append("" + accessToken.getClaimAsString(TokenClaims.EMAIL));
		response.getWriter().append("\n");
		response.getWriter().append("Family Name: ").append("" + accessToken.getClaimAsString(TokenClaims.FAMILY_NAME));
		response.getWriter().append("\n");
		response.getWriter().append("First Name: ").append("" + accessToken.getClaimAsString(TokenClaims.GIVEN_NAME));
		response.getWriter().append("\n");
		response.getWriter().append("OAuth Grant Type: ").append("" + accessToken.getGrantType());
		response.getWriter().append("\n");
		response.getWriter().append("OAuth Token: ").append("" + accessToken.getTokenValue());
		response.getWriter().append("\n");
        response.getWriter().append("SAML Assertion: ").append("" + getSAMLForXSUAA(accessToken.getTokenValue()));
		response.getWriter().append("\n");
	}

	private String getSAMLForXSUAA(String userJWTToken){

        JSONObject destinationCredentials = getCredentialsFor("destination");
        // get value of "clientid" and "clientsecret" from the environment variables
        String clientid = destinationCredentials.getString("clientid");
        String clientsecret = destinationCredentials.getString("clientsecret");
        
        // get the URL to xsuaa from the environment variables
        JSONObject xsuaaCredentials = getCredentialsFor("xsuaa");
        String xsuaaJWTToken = "";
        URI xsuaaUri = null;
        try {
            xsuaaUri = new URI(xsuaaCredentials.getString("url"));
            // use the XSUAA client library to ease the implementation of the user token exchange flow
            // XsuaaTokenFlows tokenFlows = new XsuaaTokenFlows(new DefaultOAuth2TokenService(), new XsuaaDefaultEndpoints(xsuaaUri.toString()), new ClientCredentials(clientid, clientsecret));
            
			// OAuth2ServiceConfiguration configuration = Environments.getCurrent().getXsuaaConfiguration();
            XsuaaTokenFlows tokenFlows = new XsuaaTokenFlows(new DefaultOAuth2TokenService(), new XsuaaDefaultEndpoints(xsuaaUri.toString()), new ClientCredentials(clientid, clientsecret));
			
			xsuaaJWTToken = tokenFlows.clientCredentialsTokenFlow().execute().getAccessToken();
        } catch (JSONException | URISyntaxException | IllegalArgumentException | TokenFlowException e1) {
            e1.printStackTrace();
        }
        
        URL url;
        int status = 0;
        StringBuffer content = new StringBuffer();
        try {
            //Check Destination Service API for reference: https://api.sap.com/api/SAP_CP_CF_Connectivity_Destination/resource
            url = new URL("https://destination-configuration.cfapps.eu10.hana.ondemand.com/destination-configuration/v1/destinations/saml");
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + xsuaaJWTToken);
            con.setRequestProperty("X-user-token", userJWTToken);
            status = con.getResponseCode();
        
            BufferedReader in = null;

            if (status > 299) {
                in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject obj = new JSONObject(new JSONTokener(content.toString()));
        JSONObject value = obj.getJSONArray("authTokens").getJSONObject(0);
        String token = value.getString("value");
    
        return token;
    }

	private JSONObject getCredentialsFor(String xsuaaOrDestination){
        JSONObject jsonObj = new JSONObject(System.getenv("VCAP_SERVICES"));
        JSONArray jsonArr = jsonObj.getJSONArray(xsuaaOrDestination);
        return jsonArr.getJSONObject(0).getJSONObject("credentials");
    }

}
