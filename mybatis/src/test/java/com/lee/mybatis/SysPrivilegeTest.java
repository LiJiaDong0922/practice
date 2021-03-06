package com.lee.mybatis;

import com.lee.mybatis.domain.SysPrivilege;
import com.lee.mybatis.mapper.PrivilegeMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class SysPrivilegeTest {

    @Autowired
    private PrivilegeMapper privilegeMapper;

    @Test
    public void selectById() {
        SysPrivilege sysPrivilege = privilegeMapper.selectById(1L);
        Assert.assertNotNull(sysPrivilege);
        Assert.assertEquals("用户管理", sysPrivilege.getPrivilegeName());
    }

    @Test
    public void selectPrivilegeBuRoleId(){
        List<SysPrivilege> sysPrivilegeList = privilegeMapper.selectPrivilegeBuRoleId(2L);
        Assert.assertTrue(sysPrivilegeList.size() > 0);
        sysPrivilegeList.forEach(sysPrivilege -> {
            System.out.println(sysPrivilege.getPrivilegeName());
        });
    }
}
