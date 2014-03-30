package io.gunmetal.testmocks;

/**
 * @author rees.byars
 */
public class VisibilityTestBean {

    public static Class<?> getProtected() {
        return ProtectedClass.class;
    }

    public static Class<?> getProtectedPublic() {
        return ProtectedClass.PublicClass.class;
    }

    public static Class<?> getPrivate() {
        return PrivateClass.class;
    }

    public static Class<?> getPrivatePublic() {
        return PrivateClass.PublicClass.class;
    }

    public static Class<?> getPackagePrivate() {
        return PackagePrivateClass.class;
    }

    public static Class<?> getPackagePrivatePublic() {
        return PackagePrivateClass.PublicClass.class;
    }

    public static class PublicClass {

        public static Class<?> getProtected() {
            return ProtectedClass.class;
        }

        public static Class<?> getPackagePrivate() {
            return PackagePrivateClass.class;
        }

        public static Class<?> getPrivate() {
            return PrivateClass.class;
        }

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        protected static class ProtectedClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        private static class PrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        static class PackagePrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    protected static class ProtectedClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        public static class PublicClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        private static class PrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        static class PackagePrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    private static class PrivateClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        public static class PublicClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        protected static class ProtectedClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        static class PackagePrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    static class PackagePrivateClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        public static class PublicClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        protected static class ProtectedClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        private static class PrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

}
