# Athena ADFS MFA Plugin
Plugin which extends simba jdbc `AdfsCredentialsProvider` with MFA functionality

## How to build?

```shell script
./gradlew clean build
```
## Which MFA supported
Mobile App like Microsoft Authenticator

## How it works
It sends few requests to IdP host in order to get SAML assertion

1. HTTPs GET login form
   1. process the get response to collect inputs for login form
1. HTTPs POST x-www-form-urlencoded username and password
   1. process the post response to collect inputs for mfa form (auto submit)
1. HTTPs POST x-www-form-urlencoded inputs for MFA auto submit form
   1. process response to get SAML assertion
1. assume preferred role with SAML assertion to get temporary credentials

## How to use?
1. build it first
1. add `build/libs/athena-adfs-mfa-plugin.jar` into classpath
1. add `libs/AthenaJDBC42_2.0.9.jar` into classpath
1. set jdbc url parameter `AwsCredentialsProviderClass` to `org.aba.athena.iamsupport.plugin.AdfsMfaCredentialsProvider`
1. Set other params like described at [Using ADFS Credentials Provider](https://s3.amazonaws.com/athena-downloads/drivers/JDBC/SimbaAthenaJDBC_2.0.9/docs/Simba+Athena+JDBC+Driver+Install+and+Configuration+Guide.pdf)  


## Example of jdbc connection string
```text
jdbc:awsathena://AwsRegion={region};S3OutputLocation=s3://bucket/athena/output/;AwsCredentialsProviderClass=org.aba.athena.iamsupport.plugin.AdfsMfaCredentialsProvider;idp_host=fqn.adfs.host.name;idp_port=443;User={user}@domain;Password={password};preferred_role=arn:aws:iam::1234567890:role/AthenaRole;ssl_insecure=true
```
