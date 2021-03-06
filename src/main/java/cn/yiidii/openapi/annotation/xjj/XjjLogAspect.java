package cn.yiidii.openapi.annotation.xjj;

import cn.yiidii.openapi.common.util.IPUtil;
import cn.yiidii.openapi.entity.xjj.Cdk;
import cn.yiidii.openapi.entity.xjj.XjjLog;
import cn.yiidii.openapi.xjj.service.ICdkService;
import cn.yiidii.openapi.xjj.service.IXjjLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Objects;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class XjjLogAspect {

    private final ICdkService cdkService;
    private final HttpServletRequest request;
    private final IPUtil ipUtil;
    private final IXjjLogService xjjLogService;

    @After(value = "@annotation(cn.yiidii.openapi.annotation.xjj.XjjLogAnnotation)")//已注解 @xjjLog 为切点
    public void verifyApiLimit(JoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();//注解的方法
        XjjLogAnnotation xjjLogAnnotation = method.getAnnotation(XjjLogAnnotation.class);//注解
        // 没有注解, 不记录
        if (Objects.isNull(xjjLogAnnotation)) {
            return;
        }
        String cdkStr = request.getParameter("cdk");
        Cdk cdk = cdkService.getCdkByCdk(cdkStr);
        Date now = new Date();
        Date expireTime = cdk.getExpireTime();
        if (!expireTime.after(now)) {
            // cdk到期的不记录
            return;
        }
        // 推广规则: 1.当前cdk无日志
        xjjLogService.addXjjLog(packageLog(cdk));
    }

    private XjjLog packageLog(Cdk cdk) {
        String remark = cdk.getRemark();
        remark = StringUtils.isBlank(remark) ? "" : remark;
        String ip = ipUtil.getIpAddr(request);
        String location = ipUtil.getLocationByIp(ip);
        String ua = ipUtil.getUa(request);
        XjjLog log = XjjLog.builder().cdk(cdk.getCdk()).remark(remark).ua(ua).location(location).requestTime(new Date()).build();
        return log;
    }

}
