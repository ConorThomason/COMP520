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
 15         LOADL        1
 16         CALL         fieldref
 17         LOADL        1
 18         LOAD         3[LB]
 19         CALL         fieldupd
 20         LOAD         3[LB]
 21         CALLI        L11
 22         RETURN (0)   1
 23  L11:   LOADL        10
 24         LOADA        0[OB]
 25         LOADL        0
 26         LOADL        11
 27         CALL         fieldupd
 28         LOAD         1[LB]
 29         LOADL        1
 30         CALL         fieldref
 31         LOADL        0
 32         CALL         fieldref
 33         STORE        3[LB]
 34         LOAD         3[LB]
 35         CALL         putintnl
 36         RETURN (0)   0
