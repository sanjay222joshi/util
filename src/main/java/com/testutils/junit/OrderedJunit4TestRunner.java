package com.testutils.junit;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderedJunit4TestRunner extends BlockJUnit4ClassRunner {

    public OrderedJunit4TestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> methods = new ArrayList<>(super.computeTestMethods());
        Collections.sort(methods, (fm1, fm2) -> {
            Order o1 = fm1.getAnnotation(Order.class);
            Order o2 = fm2.getAnnotation(Order.class);
            if (o1 == null || o2 == null) {
                return -1;
            }
            return o1.order() - o2.order();
        });

        return methods;
    }
}
