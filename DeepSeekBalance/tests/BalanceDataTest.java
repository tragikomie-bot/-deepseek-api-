package cn.balance.deepseek;
public class BalanceDataTest {
    static int count;
    static void check(boolean b){count++;if(!b)throw new AssertionError("case "+count);}
    static String wallet(String c,String total){return "{\"currency\":\""+c+"\",\"total_balance\":\""+total+"\",\"granted_balance\":\"10.00\",\"topped_up_balance\":\"100.00\"}";}
    static String response(boolean available,String wallets){return "{\"is_available\":"+available+",\"balance_infos\":["+wallets+"]}";}
    static void rejects(String raw){boolean failed=false;try{new BalanceData(raw);}catch(Exception e){failed=true;}check(failed);}
    public static void main(String[] args)throws Exception{
        BalanceData d=new BalanceData(response(true,wallet("CNY","110.00")));
        check(d.available);check(d.wallets.get(0).total.equals("110.00"));check(d.wallets.get(0).granted.equals("10.00"));check(d.amounts().equals("¥ 110.00"));
        d=new BalanceData(response(true,wallet("USD","0.123456")+","+wallet("CNY","12.50")));
        check(d.wallets.size()==2);check(d.amounts().equals("¥ 12.50\n$ 0.123456"));
        d=new BalanceData(response(false,wallet("CNY","0")));check(!d.available);check(d.amounts().equals("¥ 0.00"));
        check(new BalanceData(response(false,wallet("CNY","-0.25"))).amounts().equals("¥ -0.25"));
        rejects("not JSON");rejects("{}");rejects(response(true,""));rejects(response(true,wallet("CNY","NaN")));
        rejects(response(true,wallet("EUR","20")));rejects(response(true,wallet("CNY","1e999")));
        rejects(response(true,wallet("CNY","1")+","+wallet("CNY","2")));
        rejects("{\"is_available\":true,\"balance_infos\":[{\"currency\":\"CNY\",\"total_balance\":\"2\"}]}");
        System.out.println("PASS: "+count+" balance parser assertions");
    }
}
