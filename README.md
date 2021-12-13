# Description
This sample application uses the SAP Business Technology Platform (BTP) Destination Service on the Cloud Foundry environment to gererate a [SAML Assertion](https://help.sap.com/viewer/cca91383641e40ffbe03bdc78f00f681/Cloud/en-US/d81e1683bd434823abf3ceefc4ff157f.html).

# Prerequisites
- You have [Maven](https://maven.apache.org/) and [npm](https://www.npmjs.com/) installed
- Run `npm config set @sap:registry="https://npm.sap.com"`
- You have the [Cloud Foundry CLI (Command Line Interface)](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/4ef907afb1254e8286882a2bdef0edf4.html)

# Deployment on Cloud Foundry
To deploy the application, the following steps are required:
- Compile the Java application
- Create a xsuaa service instance
- Create a destination service instance
- Deploy the application
- Assign Role to your User in BTP Cockpit
- Create a destination in BTP Cockpit
- Access the application

## Compile the Java application
Run maven to package the application
```shell
mvn clean package
```

## Create the xsuaa service instance
Use the [xs-security.json](./xs-security.json) to define the authentication settings and create a service instance for XSUAA
```shell
cf create-service xsuaa application my-xsuaa -c xs-security.json
```

## Create the destination service instance
Create another service instance for the destination service
```shell
cf create-service destination lite my-destination
```

## Deploy the application
Deploy the application using cf push. It will expect 1 GB of free memory quota.

```shell
cf push --vars-file vars.yml
```

## Assign Role to your User in BTP Cockpit
Assign the deployed Role Collection `Viewer` to your user as documented [here](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/9e1bf57130ef466e8017eab298b40e5e.html).

Further up-to-date information you can get on sap.help.com:
- [Maintain Role Collections](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/d5f1612d8230448bb6c02a7d9c8ac0d1.html)
- [Maintain Roles for Applications](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/7596a0bdab4649ac8a6f6721dc72db19.html).

## Create a destination in BTP Cockpit
Create a new destination in your BTP CloudFoundry subaccount with the following settings:
Key | Value |
--- | --- |
Name | saml |
Type | HTTP |
URL | https://some.url |
Proxy Type | Internet |
Authentication | SAMLAssertion |
Audience | e.g. Local Provider Name in your backend system |
AuthnContextClassRef | urn:oasis:names:tc:SAML:2.0:ac:classes:x509 |

## Access the application
After deployment, the application router will trigger authentication. If you have assigned the role-collection provided in the [xs-security.json](./xs-security.json) to your user, you will see an output like when calling `https://approuter-<<ID>>.<<LANDSCAPE_APPS_DOMAIN>>`:

```
Client ID: sb-cfsaml!t5721
Email: user@mail
Family Name: Jones
First Name: Bob
OAuth Grant Type: authorization_code
OAuth Token: eyJhbGciOiJSUzI1NiIsInR5...
SAML Assertion: PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48c2FtbDI6QXN...
```

> Note: you can find the route of your approuter application using `cf app approuter`.

> Note: you can use a tool like [this](https://www.samltool.com/decode.php) to decode the SAML Assertion.

## Clean-Up

Finally delete your application and your service instances using the following commands:
```
cf delete -f cfsaml
cf delete -f approuter
cf delete-service -f my-xsuaa
cf delete-service -f my-destination
```
