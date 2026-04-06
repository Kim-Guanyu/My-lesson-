package com.mdkj.util;
import com.alipay.easysdk.kernel.Config;  
  

public class AlipayUtil {  
  
    /** 应用ID */  
    private static final String APPID = "9021000157614553";
    /** 异步通知接口（下单成功后支付宝回调） */  
    private static final String NOTIFY_URL = "http://646ac725.r31.cpolar.top/api/v1/order/prePayNotify";  
    /** 支付宝公钥 */  
    private static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0+elgS5sRvYaQ8vTSCTeL2X7AlAzLzQyZpL/ap0dp3L4Dl3PVTUIzYXnYQIAKgG1s3RD8kyXQgFbvgJsLzltajvqlfEtthTSl77yVUF0VampU59+7nfee2pOaZKVN0EVScTWeaLfULwVY0xMB55vDI0IXrjS0LiA8hDfnyPcUp8vDhE651Lb0c7yELPIi5+5e1mlcOTh/7ctF7WUyn10fCmyK+bxfTbzWuFJcBwfQUszkwhlGT6zCk66BDsX5GlpkrlcOcU4tNOy4VoGNt39SY4Od+XfLg/wPjuf337CahOtFnANivb4wGGhhDQighlireu4tlpjwqOu3Rt7SrUO9QIDAQAB";
    /** 应用私钥 */  
    private static final String MERCHANT_PRIVATE_KEY = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDkYZO2HABFQgTFBy7DD8a8NAacw2gu9XjrF6hf1eQcbydPnpToyC1oiV9SkfJyz+/OS4uEgfdpxM2CpgC1ZEZenyyc/6NsLnTGzYDL4tO33TFb7hBuKC5kGfd7zuekPpup3VAUUY0sPyTeDiMeTtgujtwVtQHFf+pEtVVd+VCnWT/g0hp+2MfwlFNQE7vjFk6hSL4wbqYVgu4IUQdaIsfO690MxnW63Qur7vtqtgFZMdqCq/Il6qw0+Q1WlsEkQXXqo+Ip/gZVnzDhRVTyPH+U4h1ZSpBsbobEoNlSO9J4r9hFgWQh7MBhEQIG7UXrigxSH5Kv4nvOyqhnmcIcau/TAgMBAAECggEBANfYsTaIuGi8wK1G4JdTLc1qjmVS+gCH2ES5E1WgXfvRwiGqC6sGpoQKcaEkdzSz+j3LgI04GJJKgsFcC4dkwm1jm1K2+cmhDpVmXLhWJSu9To1ILxctusq5EAJfxuaglZaxqKHLgdWzyDBmxCI8+mzmcHPDo2z6PHu7tncez2+ZTRsg14q5lh7JyUrzKCZx6AWNO7JswYbJIfxcZ1W4Mfu7trKLds2zRmzJlQCsScw+pHGq3WKQ6Ugza8/4ilKA3/0+mxZSKYPxx+Ml47Sa/77phoMTPdO/t7lXKyc0/eqy7H8hxMGchwM7hs/xl4sLo7Z+DX99oSMoEZwVMHJ4OQECgYEA/J8n+3opLX4WhdP7bSMEQBm6PSmdiZcvx3LmwQDQyFph8DkykmD5Ob5xWeO3WmtfIN+uLZxelbLBXwn7HoDGeeC+hqHPP4hHpdu75t0Me/mAxZ9G1PYtXi+3ToSnkVqgOGAMPAo3kplyU4L0/1TtHfOxezntDrxMZCirEjOjgqECgYEA529vFyaa70Wj4DZtPKQW9OPzFI+NXONTOu20Dxb6k8PnCmBn7HBhrbjaOFL2eZh/v18+15OOYvLZFX7MS+HG21AUo1f8hQMXQBPhHwQMkVhz87RrYo5rCV1aNORtCRdg6pi0xc/JpJ0a8fw9SG/yn02s+tqTq0NKhJJbw5FBUfMCgYEAvFGNBavYfIBNG/CdsorzBTHoouWd+c7JoF4oZOJNwb2W2vRYncRKpeAxMbhOm8oE/UWh24CBBglem5GeMOwAiSSgA08mZw8ZkivO1FgekWC6vRdyPxlRUrAxbwf2vqWXYM9A75USPmfDUTwDlT/jD3v1542UCXMXY2bpuETkdGECgYAAilEPE8b+0gHM7zOYFaX8EmAp5x/kXJ+AHrfx5RLWr3llm84CTkzcE/dmdEMhC4sYzkceR7joYDrDgxVFBBTrxCcYLodnZBd0zdcTtHW3DSbQJdauVzjGv9ILF5FtmQFf6tVZNN/6o2dBs14NQj2lw4+o0kPP/Ys5/gF61mb5owKBgQCz8hfly1GvNP4PN+xNQOztBQArRgYA5Qs6Q7ObVnS6C2DfiS/8zFtNT+bINUA4sbkqEclUFc6b7ZBHkKr2vew70wtFwhxPy+BZNnStRNhMNi4oNrpv+LCaa0bnJDvrHyEzynzcCSXOTkkSklcWJqnMrdtlubGOiIwUM/FpF4k0Vw==";
    /** 单例的Alipay配置对象 */  
    private static volatile Config config;  
    /** 单例对外方法 */  
    public static Config getConfig() {  
        if (config == null) {  
            synchronized (AlipayUtil.class) {  
                if (config == null) {  
                    config = new Config();  
                    config.protocol = "https";  
                    config.gatewayHost = "openapi-sandbox.dl.alipaydev.com";  
                    config.signType = "RSA2";  
                    config.ignoreSSL = true;  
                    config.appId = APPID;  
                    config.alipayPublicKey = ALIPAY_PUBLIC_KEY;  
                    config.merchantPrivateKey = MERCHANT_PRIVATE_KEY;  
                    config.notifyUrl = NOTIFY_URL;  
                }  
            }  
        }  
        return config;  
    }  
}
