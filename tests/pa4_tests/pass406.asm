  0         PUSH         0
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   LOADL        -1
  6         LOADL        2
  7         CALL         newobj  
  8         LOAD         3[LB]
  9         LOADL        1
 10         LOADL        -1
 11         LOADL        2
 12         CALL         newobj  
 13         CALL         fieldupd
 14         LOAD         3[LB]
 15         LOADL        0
 16         LOADL        5
 17         CALL         fieldupd
 18         LOAD         3[LB]
 19         LOADL        1
 20         CALL         fieldref
 21         LOADL        0
 22         LOADL        1
 23         CALL         fieldupd
 24         LOAD         3[LB]
 25         LOADL        0
 26         CALL         fieldref
 27         LOAD         3[LB]
 28         LOADL        1
 29         CALL         fieldref
 30         LOADL        0
 31         CALL         fieldref
 32         CALL         add     
 33         LOAD         4[LB]
 34         CALL         putintnl
 35         RETURN (0)   1