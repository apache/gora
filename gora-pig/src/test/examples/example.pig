set job.name 'GoraStorage test' ;
register gora/*.jar ;
webpage = LOAD '.' using org.apache.gora.pig.GoraStorage('java.lang.String','admin.WebPage','baseUrl,status,content') ;
dump webpage;
