package gg.gravesandglory;

public final class CompatibilityPolicyCheck {
    static void check(boolean pass,String why){if(!pass)throw new AssertionError(why);System.out.println("PASS "+why);}
    public static void main(String[] args){
        String build="a".repeat(64),version="0.3.2";
        check(CompatibilityPolicy.failure(version,build,1,version,build).isEmpty(),"matching release is admitted");
        check(!CompatibilityPolicy.failure(version,build,0,version,build).isEmpty(),"old protocol is refused");
        check(!CompatibilityPolicy.failure(version,build,1,"0.3.0",build).isEmpty(),"old version is refused");
        check(!CompatibilityPolicy.failure(version,build,1,version,"b".repeat(64)).isEmpty(),"same version from different source is refused");
        check(CompatibilityPolicy.required(version,build).contains("Install the same modpack release"),"missing-mod message explains how to update");
        check(CompatibilityPolicy.required(version,build).contains(version)&&CompatibilityPolicy.required(version,build).contains("aaaaaaaa"),"required version and short build identity shown");
    }
}
