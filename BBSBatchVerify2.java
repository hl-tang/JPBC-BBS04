import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Element;

//在BBS原方案上改进了签名预处理和批验证
public class BBSBatchVerify2 {
    public static void main(String[] args) {
        //1.系统初始化
        long startInitialize = System.nanoTime();
        Pairing bp = PairingFactory.getPairing("/home/hl_tang/Program_Files/jpbc-2.0.0/params/curves/f.properties");
        Field G1 = bp.getG1();
        Field G2 = bp.getG2();
        Field Zr = bp.getZr();
        //int n;
        long endInitialize = System.nanoTime();
        long timeInitialize = endInitialize - startInitialize; //单位为纳秒
        System.out.println("系统初始化时间: "+timeInitialize/(1e6)+"ms");


        //2.密钥生成
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
        System.out.print("群公钥为：");
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

        System.out.print("管理员私钥为：");
        System.out.print('(');
        System.out.print(xi_1);
        System.out.print(',');
        System.out.print(xi_2);
        System.out.println(')');

        //群成员私钥
        Element x = Zr.newRandomElement().getImmutable();
        Element gammaAddx = gamma.add(x).getImmutable();
//        System.out.println(gammaAddx);
//        System.out.println(x.add(gamma));
        Element A = g1.powZn(gammaAddx.invert()).getImmutable();
//        System.out.println(x);
//        System.out.println(A);
        System.out.print("群成员私钥为：");
        System.out.print('(');
        System.out.print(A);
        System.out.print(',');
        System.out.print(x);
        System.out.println(')');

        long endGenKey = System.nanoTime();
        long timeGenKey = endGenKey - startGenKey; //单位为纳秒
        System.out.println("密钥生成时间: "+timeGenKey/(1e6)+"ms");


        //签名预处理  与私钥A无关的都可以提前做
        Element alpha = Zr.newRandomElement().getImmutable();
        Element beta = Zr.newRandomElement().getImmutable();
        Element T1 = u.powZn(alpha).getImmutable();
        Element T2 = v.powZn(beta).getImmutable();
        Element alphaAddBeta = alpha.add(beta);
        Element hPowAlphaAddBeta = h.powZn(alphaAddBeta);

        Element r_alpha = Zr.newRandomElement().getImmutable();
        Element r_beta = Zr.newRandomElement().getImmutable();
        Element r_x = Zr.newRandomElement().getImmutable();
        Element r_delta1 = Zr.newRandomElement().getImmutable();
        Element r_delta2 = Zr.newRandomElement().getImmutable();

        Element R1 = u.powZn(r_alpha).getImmutable();
        Element R2 = v.powZn(r_beta).getImmutable();
        Element R3Part2Pair = bp.pairing(h,omega).getImmutable();
        Element R3Part3Pair = bp.pairing(h,g2).getImmutable();
        Element fu_r_alpha = r_alpha.negate().getImmutable();
        Element fu_r_beta = r_beta.negate().getImmutable();
//        System.out.println(r_alpha);
//        System.out.println(fu_r_alpha);//可以看出已经不是数论意义上的相反数了
//        System.out.println(fu_r_alpha.add(fu_r_beta));
//        System.out.println(fu_r_alpha.sub(r_beta));
        Element R3Part2 = R3Part2Pair.powZn(fu_r_alpha.add(fu_r_beta));
        Element R3Part3 = R3Part3Pair.powZn((r_delta1.negate()).sub(r_delta2));
        Element R4Part1 = T1.powZn(r_x);
        Element R4Part2 = u.powZn(r_delta1.negate());
        Element R4 = R4Part1.mul(R4Part2).getImmutable();
        Element R5Part1 = T2.powZn(r_x);
        Element R5Part2 = v.powZn(r_delta2.negate());
        Element R5 = R5Part1.mul(R5Part2).getImmutable();


        //3.签名
        long startSign = System.nanoTime();

        Element T3 = A.mul(hPowAlphaAddBeta).getImmutable();
        Element R3Part1Pair = bp.pairing(T3,g2).getImmutable();
        Element R3Part1 = R3Part1Pair.powZn(r_x);
        Element R3 = ((R3Part1.mul(R3Part2)).mul(R3Part3)).getImmutable();

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
        System.out.print("哈希值c：");
        System.out.println(c);

        Element delta1 = x.mul(alpha).getImmutable();
        Element delta2 = x.mul(beta).getImmutable();
        Element s_alpha = r_alpha.add(c.mul(alpha)).getImmutable();
        Element s_beta = r_beta.add(c.mul(beta)).getImmutable();
        Element s_x = r_x.add(c.mul(x)).getImmutable();
        Element s_delta1 = r_delta1.add(c.mul(delta1)).getImmutable();
        Element s_delta2 = r_delta2.add(c.mul(delta2)).getImmutable();

        System.out.print("签名为：");
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
        long timeSign = endSign - startSign; //单位为纳秒
        System.out.println("签名时间: "+timeSign/(1e6)+"ms");


        //4.验证
        //BatchVerify1：对R1、R2、R4、R5 4个等式验证
        long startBatchVerify1 = System.nanoTime();
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
        Element fu_s_delta1 = s_delta1.negate().getImmutable();
        Element fu_s_delta2 = s_delta2.negate().getImmutable();
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
        long endBatchVerify1 = System.nanoTime();
        long timeBatchVerify1 = endBatchVerify1 - startBatchVerify1; //单位为纳秒

        //对R3的验证优化，R3_bar的式子用双线性的性质化简，只要两次配对运算
        //BatchVerify2: 批验证R3

        long startBatchVerify2_1 = System.nanoTime();
        //c因为是不同消息做哈希,各个签名的c是不同的
        //带s的东西和c有关，所以下面这些东西每个签名都要单独计算
        //计算R3_barRefinePair1Left和R3_barRefinePair2Left的时间也应该乘num次
        Element T3PowS_x = T3.powZn(s_x).getImmutable();
        Element hPowFu_s_delta1_fu_s_delta2 = h.powZn((s_delta1.negate()).sub(s_delta2)).getImmutable();
        Element g1PowFu_c = g1.powZn(c.negate()).getImmutable();
        Element R3_barRefinePair1Left = T3PowS_x.mul(hPowFu_s_delta1_fu_s_delta2).mul(g1PowFu_c).getImmutable();

        Element hPowfu_s_alpha_fu_s_beta = h.powZn((s_alpha.negate()).sub(s_beta)).getImmutable();
        Element T3PowC = T3.powZn(c).getImmutable();
        Element R3_barRefinePair2Left = hPowfu_s_alpha_fu_s_beta.mul(T3PowC).getImmutable();
        long endBatchVerify2_1 = System.nanoTime();
        long timeBatchVerify2_1 = endBatchVerify2_1 - startBatchVerify2_1; //单位为纳秒

        long startBatchVerify2_2 = System.nanoTime();
        int num=100;  //几个签名
        Element R3_numTimes=R3;
        for(int i=1;i<=(num-1);i++){
            R3_numTimes=R3_numTimes.mul(R3);
        }
        //System.out.println(R3_numTimes);
        Element R3_barRefinePair1LeftBatch= R3_barRefinePair1Left;
        for(int i=1;i<=(num-1);i++){
            R3_barRefinePair1LeftBatch = R3_barRefinePair1LeftBatch.mul(R3_barRefinePair1Left);
        }
        Element R3_barRefinePair1Batch = bp.pairing(R3_barRefinePair1LeftBatch,g2).getImmutable();

        Element R3_barRefinePair2LeftBatch= R3_barRefinePair2Left;
        for(int i=1;i<=(num-1);i++){
            R3_barRefinePair2LeftBatch = R3_barRefinePair2LeftBatch.mul(R3_barRefinePair2Left);
        }
        Element R3_barRefinePair2Batch = bp.pairing(R3_barRefinePair2LeftBatch,omega).getImmutable();

        Element R3_barRefineBatch = R3_barRefinePair1Batch.mul(R3_barRefinePair2Batch).getImmutable();
        //System.out.println(R3_barRefineBatch);
        System.out.println(R3_barRefineBatch.isEqual(R3_numTimes));

        long endBatchVerify2_2 = System.nanoTime();
        long timeBatchVerify2_2 = endBatchVerify2_2 - startBatchVerify2_2; //单位为纳秒

        //timeBatchVerify1一定要乘num，s的一堆东西和c有关
        long timeBatchVerify = timeBatchVerify1*num + timeBatchVerify2_1*num + timeBatchVerify2_2; //单位为纳秒
        System.out.println("批验证"+num+"条签名时间: "+timeBatchVerify/(1e6)+"ms");


        //5.打开
        long startOpen = System.nanoTime();
        Element A_ = T3.div((T1.powZn(xi_1)).mul(T2.powZn(xi_2)));
//        System.out.println(A);
//        System.out.println(A_);
        long endOpen = System.nanoTime();
        long timeOpen = endOpen - startOpen; //单位为纳秒
        System.out.println("打开时间: "+timeOpen/(1e6)+"ms");
    }
}
