import { createMDX } from 'fumadocs-mdx/next';

const isProd = process.env.NODE_ENV === 'production';

/** @type {import('next').NextConfig} */
const config = {
  output: 'export',
  basePath: isProd ? '/axiom' : '',
  images: {
    unoptimized: true,
  },
  reactStrictMode: true,
};

const withMDX = createMDX();

export default withMDX(config);
