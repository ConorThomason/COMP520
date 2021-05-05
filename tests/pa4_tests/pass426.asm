  0         PUSH         0
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   LOADL        -1
  6         LOADL        1
  7         CALL         newobj  
  8         LOAD         3[LB]
  9         CALLI        L11
 10         LOADL        0
 11         LOADL        1
 12         CALL         sub     
 13         CALL         putintnl
 14         RETURN (0)   1
 15  L11:   LOADA        0[OB]
 16         LOADL        0
 17         LOADA        0[OB]
 18         CALL         fieldupd
 19         RETURN (0)   0
 20         LOAD         -1[LB]
 21         LOADL        27
 22         CALL         add     
 23         CALL         putintnl
 24         RETURN (0)   1
