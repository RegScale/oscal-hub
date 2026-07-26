// eslint-config-next 16 ships native flat configs, so the FlatCompat bridge
// (which breaks under ESLint 10) is no longer needed.
import coreWebVitals from "eslint-config-next/core-web-vitals";
import typescript from "eslint-config-next/typescript";

const asArray = (config) => (Array.isArray(config) ? config : [config]);

const eslintConfig = [
  ...asArray(coreWebVitals),
  ...asArray(typescript),
  {
    ignores: [
      "node_modules/**",
      ".next/**",
      "out/**",
      "build/**",
      "next-env.d.ts",
      "coverage/**",
      "playwright-report/**",
      "test-results/**",
    ],
  },
];

export default eslintConfig;
