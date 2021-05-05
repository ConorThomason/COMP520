  0         PUSH         0
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   LOADL        8
  6         LOAD         3[LB]
  7         CALL         newarr  
  8         LOAD         4[LB]
  9         CALL         arraylen
 10         LOAD         5[LB]
 11         CALL         putintnl
 12         RETURN (0)   1
