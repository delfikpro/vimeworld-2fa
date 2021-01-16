======================================================
Oracle Free Use Terms and Conditions (FUTC) License 
======================================================
https://www.oracle.com/downloads/licenses/oracle-free-license.html
===================================================================

ojdbc10-full.tar.gz - JDBC Thin Driver and Companion JARS
========================================================
This TAR archive (ojdbc10-full.tar.gz) contains the 19.9 release of the Oracle JDBC Thin driver(ojdbc10.jar), the Universal Connection Pool (ucp.jar) and other companion JARs grouped by category. 

(1) ojdbc10.jar (4,442,186 bytes) - 
(SHA1 Checksum: 4e65d1302ac9f3f0c01ca33ba9fe30c7b4aab537)
Oracle JDBC Driver compatible with JDK10 and JDK11; 
(2) ucp.jar (1,688,938 bytes) - (SHA1 Checksum: d240e1fc50ee1abdc1cf2334a710d23f59d30bbb)
Universal Connection Pool classes for use with JDK8, JDK9, and JDK11 -- for performance, scalability, high availability, sharded and multitenant databases.
(3) ojdbc.policy (11,515 bytes) - Sample security policy file for Oracle Database JDBC drivers

======================
Security Related JARs
======================
Java applications require some additional jars to use Oracle Wallets. 
You need to use all the three jars while using Oracle Wallets. 

(4) oraclepki.jar (311,000 bytes) - (SHA1 Checksum: 277c6b20c1627e9d82b5fb38268f4395299db049)
Additional jar required to access Oracle Wallets from Java
(5) osdt_cert.jar (210,337 bytes) - (SHA1 Checksum: a25895bdaee96f5f65afd2e0fe9cc0517775a053)
Additional jar required to access Oracle Wallets from Java
(6) osdt_core.jar (312,200 bytes) - (SHA1 Checksum: 2fa2bce450520bd7209648dc3349cc2b49d6782c)
Additional jar required to access Oracle Wallets from Java

=============================
JARs for NLS and XDK support 
=============================
(7) orai18n.jar (1,663,954 bytes) - (SHA1 Checksum: 73eb7fb62ded603619eb35e1521e96cdf38596b8) 
Classes for NLS support
(8) xdb.jar (265,130 bytes) - (SHA1 Checksum: abd556d306e6025a7fac7fe5c7a046ef27023e8b)
Classes to support standard JDBC 4.x java.sql.SQLXML interface 
(9) xmlparserv2.jar (1,933,747 bytes) - (SHA1 Checksum: 0220a0d859db8c0a15018a70d539a83b56a237f8)
Classes to support standard JDBC 4.x java.sql.SQLXML interface 
====================================================
JARs for Real Application Clusters(RAC), ADG, or DG 
====================================================
(10) ons.jar (156,242 bytes) - (SHA1 Checksum: da2cbf32647f24f2462f9cb6149374f0852341aa)
for use by the pure Java client-side Oracle Notification Services (ONS) daemon
(11) simplefan.jar (32,168 bytes) - (SHA1 Checksum: dae42f9991deccc54417f8ae129b371870a7004f)
Java APIs for subscribing to RAC events via ONS; simplefan policy and javadoc

=================
USAGE GUIDELINES
=================
Refer to the JDBC Developers Guide (https://docs.oracle.com/en/database/oracle/oracle-database/19/jjdbc/index.html) and Universal Connection Pool Developers Guide (https://docs.oracle.com/en/database/oracle/oracle-database/19/jjucp/index.html)for more details. 
