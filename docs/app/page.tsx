import Link from 'next/link';

export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-black text-white selection:bg-blue-500/30">
      {/* Background Gradients */}
      <div className="fixed inset-0 z-0 pointer-events-none">
        <div className="absolute top-[-20%] left-[-10%] h-[600px] w-[600px] rounded-full bg-purple-900/20 blur-[100px]" />
        <div className="absolute bottom-[-20%] right-[-10%] h-[600px] w-[600px] rounded-full bg-blue-900/20 blur-[100px]" />
      </div>

      <div className="container relative z-10 flex flex-col items-center justify-center gap-10 px-4 py-24 text-center">
        {/* Hero Badge */}
        <div className="inline-flex items-center rounded-full border border-zinc-800 bg-zinc-900/50 px-3 py-1 text-sm text-zinc-400 backdrop-blur-md">
          <span className="mr-2 flex h-2 w-2 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
          </span>
          v0.1.0 Released
        </div>

        {/* Hero Title */}
        <div className="flex flex-col items-center gap-6">
          <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-600 shadow-2xl shadow-blue-900/20">
            <span className="text-4xl font-bold text-white">A</span>
          </div>
          <h1 className="text-5xl font-extrabold tracking-tight sm:text-7xl bg-clip-text text-transparent bg-gradient-to-b from-white to-zinc-400">
            Axiom Engine
          </h1>
        </div>

        {/* Hero Description */}
        <p className="max-w-2xl text-xl text-zinc-400 leading-relaxed text-balance">
          The <span className="text-blue-400 font-medium">DX-first</span> Java framework for the modern era.
          <br />
          Built for Java 25. Sub-100ms startup. Zero magic.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 w-full justify-center">
          <Link
            href="/docs"
            className="inline-flex h-12 items-center justify-center rounded-lg bg-blue-600 px-8 text-sm font-semibold text-white transition-all hover:bg-blue-500 hover:shadow-lg hover:shadow-blue-500/20"
          >
            Get Started
          </Link>
          <Link
            href="https://github.com/0xtanzim/axiom"
            className="inline-flex h-12 items-center justify-center rounded-lg border border-zinc-800 bg-zinc-900 px-8 text-sm font-semibold text-white transition-all hover:bg-zinc-800 hover:border-zinc-700"
          >
            View on GitHub
          </Link>
        </div>

        {/* Code Snippet */}
        <div className="mt-12 w-full max-w-2xl overflow-hidden rounded-xl border border-zinc-800 bg-black/50 shadow-2xl backdrop-blur-sm transform transition-all hover:border-zinc-700">
          <div className="flex items-center gap-2 border-b border-zinc-800 bg-zinc-900/50 px-4 py-3">
            <div className="h-3 w-3 rounded-full bg-red-500/20 border border-red-500/50" />
            <div className="h-3 w-3 rounded-full bg-yellow-500/20 border border-yellow-500/50" />
            <div className="h-3 w-3 rounded-full bg-green-500/20 border border-green-500/50" />
            <span className="ml-2 text-xs font-mono text-zinc-500">App.java</span>
          </div>
          <div className="p-6 text-left font-mono text-sm overflow-x-auto">
            <span className="text-purple-400">Axiom</span>.create()
            <br />
            &nbsp;&nbsp;.<span className="text-blue-400">get</span>(<span className="text-green-400">"/"</span>, ctx -&gt; ctx.text(<span className="text-green-400">"Hello, World!"</span>))
            <br />
            &nbsp;&nbsp;.listen(<span className="text-orange-400">8080</span>);
          </div>
        </div>

        {/* Feature Grid */}
        <div className="mt-20 grid gap-6 sm:grid-cols-3 w-full max-w-5xl">
          <FeatureCard
            icon="🚀"
            title="One Dependency"
            description="Just add axiom to your project. No starters, no BOMs, no parent POMs."
          />
          <FeatureCard
            icon="⚡"
            title="Modern Java 25"
            description="Virtual threads, records, pattern matching by default. No legacy baggage."
          />
          <FeatureCard
            icon="🔧"
            title="Explicit Over Magic"
            description="No classpath scanning. No runtime reflection. You control everything."
          />
        </div>
      </div>
    </main>
  );
}

function FeatureCard({ icon, title, description }: { icon: string; title: string; description: string }) {
  return (
    <div className="group rounded-xl border border-zinc-800 bg-zinc-900/30 p-6 text-left transition-all hover:border-zinc-700 hover:bg-zinc-900/50">
      <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-lg bg-zinc-800/50 text-2xl group-hover:scale-110 transition-transform">
        {icon}
      </div>
      <h3 className="mb-2 text-lg font-semibold text-white">{title}</h3>
      <p className="text-sm text-zinc-400 leading-relaxed">{description}</p>
    </div>
  );
}
