import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Element;

public class BBS {
    public static void main(String[] args) {
        //1.System Initialization
        long startInitialize = System.nanoTime();
        //f类椭圆曲线生成非对称的双线性群，配对运算时间长，安全性更强
        Pairing bp = PairingFactory.getPairing("f.properties");
        Field G1 = bp.getG1();
        Field G2 = bp.getG2();
        Field Zr = bp.getZr();
        //int n;
        long endInitialize = System.nanoTime();
        long timeInitialize = endInitialize - startInitialize; //unit is nanosecond
        System.out.println("System Initialization time: "+timeInitialize/(1e6)+"ms");


        //2.Key Generation
        long startGenKey = System.nanoTime();

        Element g1 = G1.newRandomElement().getImmutable();
        Element g2 = G2.newRandomElement().getImmutable();
        Element h = G1.newRandomElement().getImmutable();
        Element xi_1 = Zr.newRandomElement().getImmutable();
        Element xi_2 = Zr.newRandomElement().getImmutable();
        Element u=h.powZn(xi_1.invert()).getImmutable();
        Element v=h.powZn(xi_2.invert()).getImmutable();
        /*System.out.println(h);
        System.out.println(u.powZn(xi_1));
        System.out.println(v.powZn(xi_2));
        System.out.println((u.powZn(xi_1)).isEqual(v.powZn(xi_2)));*/
        Element gamma = Zr.newRandomElement().getImmutable();
        Element omega = g2.powZn(gamma).getImmutable();
        System.out.print("group public key：");
        System.out.print('(');
        System.out.print(g1);
        System.out.print(',');
        System.out.print(g2);
        System.out.print(',');
        System.out.print(h);
        System.out.print(',');
        System.out.print(u);
        System.out.print(',');
        System.out.print(v);
        System.out.print(',');
        System.out.print(omega);
        System.out.println(')');

        System.out.print("manger private key：");
        System.out.print('(');
        System.out.print(xi_1);
        System.out.print(',');
        System.out.print(xi_2);
        System.out.println(')');

        //member private key
        Element x = Zr.newRandomElement().getImmutable();
        Element gammaAddx = gamma.add(x).getImmutable();
//        System.out.println(gammaAddx);
//        System.out.println(x.add(gamma));
        Element A = g1.powZn(gammaAddx.invert()).getImmutable();
        //check if private key forged by non-group member can succeed to pass verification
        //Element A = G1.newRandomElement().getImmutable(); //verification failed
//        System.out.println(x);
//        System.out.println(A);
        System.out.print("group member private key：");
        System.out.print('(');
        System.out.print(A);
        System.out.print(',');
        System.out.print(x);
        System.out.println(')');

        long endGenKey = System.nanoTime();
        long timeGenKey = endGenKey - startGenKey; //unit is nanosecond
        System.out.println("Key Generation time: "+timeGenKey/(1e6)+"ms");


        //3.Signing
        long startSign = System.nanoTime();

        Element alpha = Zr.newRandomElement().getImmutable();
        Element beta = Zr.newRandomElement().getImmutable();
        Element T1 = u.powZn(alpha).getImmutable();
        Element T2 = v.powZn(beta).getImmutable();
        Element alphaAddBeta = alpha.add(beta);
        Element hPowAlphaAddBeta = h.powZn(alphaAddBeta);
        Element T3 = A.mul(hPowAlphaAddBeta).getImmutable();

        Element r_alpha = Zr.newRandomElement().getImmutable();
        Element r_beta = Zr.newRandomElement().getImmutable();
        Element r_x = Zr.newRandomElement().getImmutable();
        Element r_delta1 = Zr.newRandomElement().getImmutable();
        Element r_delta2 = Zr.newRandomElement().getImmutable();

        Element R1 = u.powZn(r_alpha).getImmutable();
        Element R2 = v.powZn(r_beta).getImmutable();
        Element R3Part1Pair = bp.pairing(T3,g2).getImmutable();
        Element R3Part2Pair = bp.pairing(h,omega).getImmutable();
        Element R3Part3Pair = bp.pairing(h,g2).getImmutable();
        Element R3Part1 = R3Part1Pair.powZn(r_x);
        Element fu_r_alpha = r_alpha.negate().getImmutable();
        Element fu_r_beta = r_beta.negate().getImmutable();
//        System.out.println(r_alpha);
//        System.out.println(fu_r_alpha);//可以看出已经不是数论意义上的相反数了
//        System.out.println(fu_r_alpha.add(fu_r_beta));
//        System.out.println(fu_r_alpha.sub(r_beta));
        Element R3Part2 = R3Part2Pair.powZn(fu_r_alpha.add(fu_r_beta));
        Element R3Part3 = R3Part3Pair.powZn((r_delta1.negate()).sub(r_delta2));
        Element R3 = ((R3Part1.mul(R3Part2)).mul(R3Part3)).getImmutable();
        Element R4Part1 = T1.powZn(r_x);
        Element R4Part2 = u.powZn(r_delta1.negate());
        Element R4 = R4Part1.mul(R4Part2).getImmutable();
        Element R5Part1 = T2.powZn(r_x);
        Element R5Part2 = v.powZn(r_delta2.negate());
        Element R5 = R5Part1.mul(R5Part2).getImmutable();

        String M="message";
        String M_sign=M;
        M_sign+=T1;
        M_sign+=T2;
        M_sign+=T3;
        M_sign+=R1;
        M_sign+=R2;
        M_sign+=R3;
        M_sign+=R4;
        M_sign+=R5;
        //System.out.println(M_sign.hashCode());
        int c_sign=M_sign.hashCode();
        //System.out.println(c_sign);
        //c_sign有可能是负的，而且powZn()的参数是Element类型，不是int类型
        //要从哈希映射回Z群，转换为Element类型
        //System.out.println(Integer.toString(c_sign));
        byte[] c_sign_byte = Integer.toString(c_sign).getBytes();
        Element c = (Zr.newElementFromHash(c_sign_byte, 0, c_sign_byte.length)).getImmutable();
        //System.out.println(c);
        System.out.print("hash value c：");
        System.out.println(c);

        Element delta1 = x.mul(alpha).getImmutable();
        Element delta2 = x.mul(beta).getImmutable();
        Element s_alpha = r_alpha.add(c.mul(alpha)).getImmutable();
        Element s_beta = r_beta.add(c.mul(beta)).getImmutable();
        Element s_x = r_x.add(c.mul(x)).getImmutable();
        Element s_delta1 = r_delta1.add(c.mul(delta1)).getImmutable();
        Element s_delta2 = r_delta2.add(c.mul(delta2)).getImmutable();

        System.out.print("signature：");
        System.out.print('(');
        System.out.print(T1);
        System.out.print(',');
        System.out.print(T2);
        System.out.print(',');
        System.out.print(T3);
        System.out.print(',');
        System.out.print(c);
        System.out.print(',');
        System.out.print(s_alpha);
        System.out.print(',');
        System.out.print(s_beta);
        System.out.print(',');
        System.out.print(s_x);
        System.out.print(',');
        System.out.print(s_delta1);
        System.out.print(',');
        System.out.print(s_delta2);
        System.out.println(')');

        long endSign = System.nanoTime();
        long timeSign = endSign - startSign; //unit is nanosecond
        System.out.println("Signing time: "+timeSign/(1e6)+"ms");


        //4.Verification
        long startVerify = System.nanoTime();

        Element R1_barPart1 = u.powZn(s_alpha);
        Element R1_barPart2 = T1.powZn(c.negate());
        Element R1_bar = R1_barPart1.mul(R1_barPart2).getImmutable();
        //System.out.println(R1);
        //System.out.println(R1_bar);
        //System.out.println(R1_bar.isEqual(R1));
        Element R2_barPart1 = v.powZn(s_beta);
        Element R2_barPart2 = T2.powZn(c.negate());
        Element R2_bar = R2_barPart1.mul(R2_barPart2).getImmutable();
        //System.out.println(R2);
        //System.out.println(R2_bar);
        //System.out.println(R2_bar.isEqual(R2));
        Element R3_barPart1Pair = bp.pairing(T3,g2).getImmutable();
        Element R3_barPart2Pair = bp.pairing(h,omega).getImmutable();
        Element R3_barPart3Pair = bp.pairing(h,g2).getImmutable();
        Element R3_barPart1 = R3_barPart1Pair.powZn(s_x);
        Element R3_barPart2 = R3_barPart2Pair.powZn((s_alpha.negate()).sub(s_beta));
        Element fu_s_delta1 = s_delta1.negate().getImmutable();
        Element fu_s_delta2 = s_delta2.negate().getImmutable();
        Element R3_barPart3 = R3_barPart3Pair.powZn(fu_s_delta1.add(fu_s_delta2));
        Element e_T3_omega = bp.pairing(T3,omega).getImmutable();
        Element e_g1_g2 = bp.pairing(g1,g2).getImmutable();
        Element R3_barPart4 = (e_T3_omega.div(e_g1_g2)).powZn(c);
        Element R3_bar = R3_barPart1.mul(R3_barPart2).mul(R3_barPart3).mul(R3_barPart4).getImmutable();
        //System.out.println(R3);
        //System.out.println(R3_bar);
        //System.out.println(R3_bar.isEqual(R3));
        Element R4_barPart1 = T1.powZn(s_x);
        Element R4_barPart2 = u.powZn(fu_s_delta1);
        Element R4_bar = R4_barPart1.mul(R4_barPart2).getImmutable();
        //System.out.println(R4);
        //System.out.println(R4_bar);
        //System.out.println(R4_bar.isEqual(R4));
        Element R5_barPart1 = T2.powZn(s_x);
        Element R5_barPart2 = v.powZn(fu_s_delta2);
        Element R5_bar = R5_barPart1.mul(R5_barPart2).getImmutable();
        //System.out.println(R5);
        //System.out.println(R5_bar);
        //System.out.println(R5_bar.isEqual(R5));

        String M_verify=M;
        M_verify+=T1;
        M_verify+=T2;
        M_verify+=T3;
        M_verify+=R1_bar;
        M_verify+=R2_bar;
        M_verify+=R3_bar;
        M_verify+=R4_bar;
        M_verify+=R5_bar;
        int c_verify=M_verify.hashCode();
        byte[] c_verify_byte = Integer.toString(c_verify).getBytes();
        Element c_ = (Zr.newElementFromHash(c_verify_byte, 0, c_verify_byte.length)).getImmutable();
        System.out.print("hash value c_：");
        System.out.println(c_);
        if(c_.isEqual(c)){
            System.out.println("succeed to verify");
        }
        else{
            System.out.println("fail to verify");
        }

        long endVerify = System.nanoTime();
        long timeVerify = endVerify - startVerify; //unit is nanosecond
        System.out.println("Verification time: "+timeVerify/(1e6)+"ms");


        //5.Open the group signature to identify the signer
        long startOpen = System.nanoTime();
        Element A_ = T3.div((T1.powZn(xi_1)).mul(T2.powZn(xi_2)));
//        System.out.println(A);
//        System.out.println(A_);
        long endOpen = System.nanoTime();
        long timeOpen = endOpen - startOpen; //unit is nanosecond
        System.out.println("Open time: "+timeOpen/(1e6)+"ms");
    }
}
