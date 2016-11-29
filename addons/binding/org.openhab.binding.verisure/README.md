Verisure Binding

This is an OpenHAB binding for Versiure Alarm system, by Securitas Direct

This binding uses the rest API behind the myverisure pages https://mypages.verisure.com/login.html
Be aware that they don't approve if you update to often, I have gotten no complaints running with a 10 minutes update interval, but officially you should use 30 minutes.

Supported Things

This binding supports the following thing types:

ClimateSensor
Yaleman Doorlock
The Alarm Status on the bridge

Binding Configuration
You will have to configure the bridge with an auth string.
You should get the auth string from your browser when you login to myverisure. It is basically the username and password.
You use the developer tools in chrome and then you look at the https://mypages.verisure.com/j_spring_security_check request. In this you will find something like this in the content: 
j_username=XXXX&j_password=YYYY. this is what you should use in the auth string.

Discovery
NA

Thing Configuration
TBD

Channels

TBD

Full Example

TBD
