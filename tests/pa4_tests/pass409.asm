  0         PUSH         0
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   LOADL        4
  6         LOAD         3[LB]
  7         CALL         newarr  
  8         LOADL        1
  9         LOAD         4[LB]
 10         LOADL        0
 11         LOAD         5[LB]
 12         CALL         arrayupd
 13         JUMP         L12
 14  L11:   LOAD         4[LB]
 15         LOAD         5[LB]
 16         LOAD         4[LB]
 17         LOAD         5[LB]
 18         LOADL        1
 19         CALL         sub     
 20         CALL         arrayref
 21         LOAD         5[LB]
 22         CALL         add     
 23         CALL         arrayupd
 24         LOAD         5[LB]
 25         LOADL        1
 26         CALL         add     
 27         STORE        5[LB]
 28  L12:   LOAD         5[LB]
 29         LOAD         4[LB]
 30         CALL         arraylen
 31         CALL         lt      
 32         JUMPIF (1)   L11
 33         LOAD         4[LB]
 34         LOADL        3
 35         CALL         arrayref
 36         LOADL        2
 37         CALL         add     
 38         LOAD         6[LB]
 39         CALL         putintnl
 40         RETURN (0)   1
