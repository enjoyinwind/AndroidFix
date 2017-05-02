# AndroidFix
基于rocoofix的热修复，进行了一些扩展：
1.目前不打开混淆，无法正常工作；
2.某类不进行hack注入时，其内部类也不应该进行注入；
3.增加对excludePackage的支持，在此包名下的class都不inject；
4.支持对app使用Android annotation框架生成的class文件的处理，因为源码文件不变，annotation生成的文件可能会变；
5.增加签名验证；
6.patch目录每次重新生成，防止残留文件影响补丁包；
