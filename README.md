# BUW-Secure-SMS

BUW Secure SMS Encrypter is a ligthweight SMS encryption application for android operating system. It was developped at the Bauhaus-University of Weimar (Germany, Thuringia) on [chair of media security](http://www.uni-weimar.de/cms/medien/mediensicherheit/home.html) at the faculty of media.

It encryptes your SMS authenticated with an capacity of 136/146 chars per message (1st/n-th). For it, it uses the authenticated encryption scheme [McOE](http://eprint.iacr.org/2011/644) and the AES-128 blockcipher.

BUW Secure SMS Encrypter needs at least Android 2.1 and was tested in all german GSM networks. All parts of the encryption process were tested by JUnit tests.

To run BUW Secure SMS Encrypter you have to exchange one shared key between your and the device of your communication partner. For every partner you want to communicate with. It is not applicalable to use BUW Sevcure SMS Encrypter for unencrypted SMS .

* * *

To compile BUW Secure SMS Encrypter, you additionally need two external Libraries:

* ZXing 2.1 or above to include in the project, Download from [ZXING download page](http://code.google.com/p/zxing/downloads/list)
* SkeinThreefish from the official [Skein Threefish Webpage](https://github.com/wernerd/Skein3Fish/tree/master/java)
