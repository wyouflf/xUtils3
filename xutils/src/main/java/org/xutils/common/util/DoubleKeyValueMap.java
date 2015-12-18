/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xutils.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: wyouflf
 * Date: 13-6-19
 * Time: PM 1:18
 */
public class DoubleKeyValueMap<K1, K2, V> {

    private final ConcurrentHashMap<K1, ConcurrentHashMap<K2, V>> k1_k2V_map;

    public DoubleKeyValueMap() {
        this.k1_k2V_map = new ConcurrentHashMap<K1, ConcurrentHashMap<K2, V>>();
    }

    public void put(K1 key1, K2 key2, V value) {
        if (key1 == null || key2 == null || value == null) return;
        if (k1_k2V_map.containsKey(key1)) {
            ConcurrentHashMap<K2, V> k2V_map = k1_k2V_map.get(key1);
            if (k2V_map != null) {
                k2V_map.put(key2, value);
            } else {
                k2V_map = new ConcurrentHashMap<K2, V>();
                k2V_map.put(key2, value);
                k1_k2V_map.put(key1, k2V_map);
            }
        } else {
            ConcurrentHashMap<K2, V> k2V_map = new ConcurrentHashMap<K2, V>();
            k2V_map.put(key2, value);
            k1_k2V_map.put(key1, k2V_map);
        }
    }

    public Set<K1> getFirstKeys() {
        return k1_k2V_map.keySet();
    }

    public ConcurrentHashMap<K2, V> get(K1 key1) {
        return k1_k2V_map.get(key1);
    }

    public V get(K1 key1, K2 key2) {
        ConcurrentHashMap<K2, V> k2_v = k1_k2V_map.get(key1);
        return k2_v == null ? null : k2_v.get(key2);
    }

    public Collection<V> getAllValues(K1 key1) {
        ConcurrentHashMap<K2, V> k2_v = k1_k2V_map.get(key1);
        return k2_v == null ? null : k2_v.values();
    }

    public Collection<V> getAllValues() {
        Collection<V> result = null;
        Set<K1> k1Set = k1_k2V_map.keySet();
        if (k1Set != null) {
            result = new ArrayList<V>();
            for (K1 k1 : k1Set) {
                Collection<V> values = k1_k2V_map.get(k1).values();
                if (values != null) {
                    result.addAll(values);
                }
            }
        }
        return result;
    }

    public boolean containsKey(K1 key1, K2 key2) {
        if (k1_k2V_map.containsKey(key1)) {
            return k1_k2V_map.get(key1).containsKey(key2);
        }
        return false;
    }

    public boolean containsKey(K1 key1) {
        return k1_k2V_map.containsKey(key1);
    }

    public int size() {
        if (k1_k2V_map.size() == 0) return 0;

        int result = 0;
        for (ConcurrentHashMap<K2, V> k2V_map : k1_k2V_map.values()) {
            result += k2V_map.size();
        }
        return result;
    }

    public void remove(K1 key1) {
        k1_k2V_map.remove(key1);
    }

    public void remove(K1 key1, K2 key2) {
        ConcurrentHashMap<K2, V> k2_v = k1_k2V_map.get(key1);
        if (k2_v != null) {
            k2_v.remove(key2);
        }
        if (k2_v == null || k2_v.isEmpty()) {
            k1_k2V_map.remove(key1);
        }
    }

    public void clear() {
        if (k1_k2V_map.size() > 0) {
            for (ConcurrentHashMap<K2, V> k2V_map : k1_k2V_map.values()) {
                k2V_map.clear();
            }
            k1_k2V_map.clear();
        }
    }
}
