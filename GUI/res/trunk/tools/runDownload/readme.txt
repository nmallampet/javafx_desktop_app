#### In .pad file
Pinout by Port Name:
+-----------+----------+--------------+-------+-----------+-----------------------------------+
| Port Name | Pin/Bank | Buffer Type  | Site  | BC Enable | Properties                        |
+-----------+----------+--------------+-------+-----------+-----------------------------------+
| Aclr      | N16/8    | LVCMOS25_IN  | PB4B  |           | PULL:DOWN CLAMP:ON HYSTERESIS:ON  |
| Clk_En    | G3/3     | LVCMOS25_IN  | PR29B |           | PULL:DOWN CLAMP:ON HYSTERESIS:ON  |
| Clock     | B10/0    | LVCMOS25_IN  | PT29A |           | PULL:DOWN CLAMP:ON HYSTERESIS:ON  |
| Q[0]      | F1/3     | LVCMOS25_OUT | PR35C |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[1]      | F2/3     | LVCMOS25_OUT | PR32A |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[2]      | H1/3     | LVCMOS25_OUT | PR35D |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[3]      | J2/3     | LVCMOS25_OUT | PR35A |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[4]      | J3/3     | LVCMOS25_OUT | PR29C |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[5]      | H3/3     | LVCMOS25_OUT | PR29A |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[6]      | H2/3     | LVCMOS25_OUT | PR29D |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
| Q[7]      | G1/3     | LVCMOS25_OUT | PR32B |           | DRIVE:8mA CLAMP:ON SLEW:SLOW      |
+-----------+----------+--------------+-------+-----------+-----------------------------------+


#### In lst file
       ps /sim_top/uut/Clock /sim_top/uut/Clk_En /sim_top/uut/Aclr /sim_top/uut/Q
        0                  X                   X                 X       XXXXXXXX
     5000                  X                   X                 X       XXXXXXXX
    10000                  X                   X                 X       XXXXXXXX
    15000                  0                   0                 1       00000000
    20000                  0                   0                 1       00000000
    25000                  0                   0                 1       00000000
    30000                  0                   0                 1       00000000



