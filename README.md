# JPBC-BBS04
Using JPBC(Java Pairing-Based Cryptography Library) to implement pairing-based cryptography algorithm like BBS04 group signature. And I proposed a modification of batch verification to the original BBS04.



## How to use JPBC





## BBS04 group signature scheme





## A proposal of batch verification

### An improtant property of bilinear pairing

$$
e(ab,c)=e(a,c)e(b,c)
$$

prove:

let $a=g^{\alpha},b=g^{\beta},c=g^\gamma$

$e(g^{\alpha}g^{\beta},g^\gamma)$

$=e(g^{\alpha+\beta},g^\gamma)$

$=e(g,g)^{(\alpha+\beta)\gamma}$

$=e(g,g)^{\alpha\gamma+\beta\gamma}$

$=e(g,g)^{\alpha\gamma} \cdot e(g,g)^{\beta\gamma}$

$=e(g^\alpha,g^\gamma)e(g^\beta,g^\gamma)$

When computing two bilinear pairings with the same $c$, we can only compute one bilinear pairing through the property, thus decreasing the computation of bilinear pairing, which is far more time-consuming than multiplication.




### batch verification of BBS04 scheme
