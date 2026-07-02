package com.rbac.base.modules.system.service;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.modules.system.entity.SysConfig;
import com.rbac.base.modules.system.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysConfigService {

    @Autowired
    private SysConfigMapper configMapper;

    // 16字节 AES 密钥
    private static final byte[] AES_KEY = "tx_ticket_123456".getBytes();
    private static final AES aes = SecureUtil.aes(AES_KEY);

    /**
     * 根据键获取未加密的普通值
     */
    public String getConfigValue(String key) {
        return configMapper.getConfigValue(key);
    }

    /**
     * 根据键获取解密后的值 (如果数据库中存的是密文)
     */
    public String getDecryptedConfigValue(String key) {
        String value = configMapper.getConfigValue(key);
        if (value != null && !value.isEmpty()) {
            try {
                return aes.decryptStr(value);
            } catch (Exception e) {
                return value; // 兼容非密文历史数据
            }
        }
        return value;
    }

    /**
     * 获取指定前缀的所有配置，并解密敏感信息
     */
    public Map<String, String> getConfigsByPrefix(String prefix) {
        List<SysConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .likeRight(SysConfig::getConfigKey, prefix));
        Map<String, String> map = new HashMap<>();
        for (SysConfig config : configs) {
            String val = config.getConfigValue();
            if (config.getConfigKey().contains("Secret") || config.getConfigKey().contains("password")) {
                if (val != null && !val.isEmpty()) {
                    try {
                        val = aes.decryptStr(val);
                    } catch (Exception ignored) {}
                }
            }
            map.put(config.getConfigKey(), val);
        }
        return map;
    }

    /**
     * 批量保存配置
     */
    public void saveConfigs(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 敏感信息加密存储
            if (key.contains("Secret") || key.contains("password")) {
                if (value != null && !value.isEmpty() && !value.equals("********")) {
                    value = aes.encryptHex(value);
                } else if (value != null && value.equals("********")) {
                    continue; // 不修改
                }
            }

            SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, key));

            if (config != null) {
                config.setConfigValue(value);
                configMapper.updateById(config);
            } else {
                config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                configMapper.insert(config);
            }
        }
    }
}
