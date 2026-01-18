import type { BaseLayoutProps } from 'fumadocs-ui/layouts/shared';

export function baseOptions(): BaseLayoutProps {
  return {
    nav: {
      title: (
        <div className="flex items-center gap-2 font-bold">
          <div className="flex h-6 w-6 items-center justify-center rounded bg-gradient-to-br from-blue-500 to-purple-600 text-xs font-bold text-white">
            A
          </div>
          Axiom
        </div>
      ),
    },
    links: [
      {
        text: 'Documentation',
        url: '/docs',
        active: 'nested-url',
      },
      {
        text: 'GitHub',
        url: 'https://github.com/0xtanzim/axiom',
      },
    ],
  };
}
