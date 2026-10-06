import { Briefcase, FileText, User, Target, Upload, FileCheck, ArrowRight } from 'lucide-react';

interface LandingPageProps {
  onNavigate: (view: 'login' | 'register') => void;
}

export default function LandingPage({ onNavigate }: LandingPageProps) {
  return (
    <div className="min-h-screen bg-[#f8faff] flex flex-col font-sans">
      {/* Header */}
      <header className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 bg-transparent">
        <div className="flex h-20 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-2 cursor-pointer">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600 text-white shadow-blue-200 shadow-lg transform -rotate-12">
              <svg className="h-5 w-5 fill-current" viewBox="0 0 24 24">
                <path d="M2 21l21-9L2 3v7l15 2-15 2v7z" />
              </svg>
            </div>
            <span className="text-2xl font-bold tracking-tight text-[#0f172a]">
              Job<span className="text-blue-600">Pilot</span>
            </span>
          </div>

          {/* Nav Links */}
          <nav className="hidden md:flex items-center gap-8">
            <a href="#about" className="text-sm font-semibold text-slate-600 hover:text-blue-600 transition-colors">
              About
            </a>
            <a href="#features" className="text-sm font-semibold text-slate-600 hover:text-blue-600 transition-colors">
              Features
            </a>
          </nav>

          {/* CTA Buttons */}
          <div className="flex items-center gap-4">
            <button
              onClick={() => onNavigate('login')}
              className="rounded-lg border border-blue-600/30 py-2 px-5 text-sm font-bold text-blue-600 hover:bg-blue-50 transition-all"
            >
              Login
            </button>
            <button
              onClick={() => onNavigate('register')}
              className="rounded-lg bg-blue-600 py-2 px-5 text-sm font-bold text-white hover:bg-blue-700 shadow-md shadow-blue-200 transition-all"
            >
              Sign Up
            </button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 py-12 md:py-20 flex-grow flex flex-col lg:flex-row items-center gap-12">
        {/* Left Hero */}
        <div className="flex-1 space-y-6 text-left">
          <div className="inline-flex items-center gap-2 rounded-full bg-blue-50 py-1.5 px-4 text-xs font-bold text-blue-600 border border-blue-100">
            <span className="flex h-2 w-2 rounded-full bg-blue-600 animate-pulse"></span>
            Your Job Search Co-Pilot
          </div>

          <h1 className="text-5xl md:text-6.5xl font-extrabold tracking-tight text-[#0f172a] leading-[1.15]">
            Simplify Your <br />
            <span className="text-blue-600">Job Search</span>
          </h1>

          <p className="text-lg text-slate-600 max-w-lg leading-relaxed">
            Upload your resume, manage your profile, and get one step closer to your next opportunity.
          </p>

          <div className="flex flex-wrap gap-4 pt-2">
            <button
              onClick={() => onNavigate('register')}
              className="flex items-center gap-2 rounded-lg bg-blue-600 py-3.5 px-7 text-base font-bold text-white hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all hover:translate-x-0.5"
            >
              Get Started
              <ArrowRight className="h-5 w-5" />
            </button>
            <button
              onClick={() => onNavigate('login')}
              className="rounded-lg border border-blue-600/30 py-3.5 px-7 text-base font-bold text-blue-600 hover:bg-blue-50 transition-all"
            >
              Login
            </button>
          </div>
        </div>

        {/* Right Hero (Gorgeous CSS SVG Illustration matching mockup!) */}
        <div className="flex-1 w-full relative flex items-center justify-center lg:justify-end">
          <div className="relative w-full max-w-[500px] aspect-[4/3] bg-transparent">
            {/* Background elements */}
            <div className="absolute inset-0 bg-blue-100/40 rounded-full filter blur-3xl scale-95 -z-10"></div>

            {/* Central illustration: Developer at Desk */}
            <div className="absolute left-[15%] bottom-[5%] w-[70%] h-[75%] bg-transparent flex items-end justify-center">
              <svg className="w-full h-full text-slate-800" viewBox="0 0 400 300" fill="none">
                {/* Desk/Laptop */}
                <rect x="100" y="240" width="200" height="10" rx="3" fill="#cbd5e1" />
                <path d="M120 240l10-40h140l10 40H120z" fill="#94a3b8" opacity="0.3" />
                <rect x="140" y="160" width="120" height="80" rx="4" fill="#334155" />
                <rect x="145" y="165" width="110" height="70" rx="2" fill="#f8fafc" />
                <circle cx="200" cy="200" r="4" fill="#cbd5e1" />
                
                {/* Programmer */}
                <path d="M170 240c0-30 10-50 30-50s30 20 30 50H170z" fill="#3b82f6" />
                <circle cx="200" cy="155" r="22" fill="#fed7aa" />
                {/* Hair */}
                <path d="M178 150c0-15 10-25 22-25s22 10 22 25c0 3-5 5-10 2s-10-5-14-1-20-1-20-1z" fill="#1e293b" />
                {/* Face details */}
                <circle cx="193" cy="155" r="2" fill="#1e293b" />
                <circle cx="207" cy="155" r="2" fill="#1e293b" />
                <path d="M197 165c2 2 4 2 6 0" stroke="#1e293b" strokeWidth="2" strokeLinecap="round" />
                {/* Hands */}
                <path d="M165 240c10-10 20-5 25-2m50 2c-10-10-20-5-25-2" stroke="#fed7aa" strokeWidth="6" strokeLinecap="round" />
              </svg>
            </div>

            {/* Float Card 1: Resume document with check */}
            <div className="absolute left-[10%] top-[10%] w-[50%] bg-white rounded-2xl shadow-xl border border-slate-100 p-4 flex flex-col gap-2 transform -rotate-6 transition-all hover:scale-105 duration-300">
              <div className="flex items-center justify-between">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
                  <FileText className="h-4 w-4" />
                </div>
                <div className="h-6 w-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
                  <svg className="h-3.5 w-3.5 stroke-[3]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              </div>
              <div className="space-y-1.5 pt-1">
                <div className="h-2 w-3/4 rounded bg-slate-200"></div>
                <div className="h-2 w-full rounded bg-slate-100"></div>
                <div className="h-2 w-5/6 rounded bg-slate-100"></div>
              </div>
            </div>

            {/* Float Floating Action Pill 1: Upload Resume */}
            <div className="absolute right-[5%] top-[15%] bg-white rounded-xl shadow-lg border border-slate-50 py-3 px-4 flex items-center gap-3 transform translate-x-2 transition-all hover:scale-105 duration-300">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-50 text-blue-600">
                <Upload className="h-4 w-4" />
              </div>
              <span className="text-xs font-bold text-slate-800">Upload Resume</span>
            </div>

            {/* Float Floating Action Pill 2: Manage Profile */}
            <div className="absolute right-[0%] top-[43%] bg-white rounded-xl shadow-lg border border-slate-50 py-3 px-4 flex items-center gap-3 transition-all hover:scale-105 duration-300">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-50 text-blue-600">
                <FileCheck className="h-4 w-4" />
              </div>
              <span className="text-xs font-bold text-slate-800">Manage Profile</span>
            </div>

            {/* Float Floating Action Pill 3: Apply to Jobs */}
            <div className="absolute right-[5%] bottom-[15%] bg-white rounded-xl shadow-lg border border-slate-50 py-3 px-4 flex items-center gap-3 transform translate-x-4 transition-all hover:scale-105 duration-300">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-50 text-blue-600">
                <Briefcase className="h-4 w-4" />
              </div>
              <span className="text-xs font-bold text-slate-800">Apply to Jobs</span>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Cards Section */}
      <section id="features" className="bg-white border-t border-slate-100 py-16 md:py-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
            {/* Feature 1 */}
            <div className="flex flex-col items-start text-left bg-slate-50/50 p-8 rounded-2xl border border-slate-100/50 hover:shadow-md transition-shadow">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-blue-100 text-blue-600 mb-6">
                <FileText className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Upload Resume</h3>
              <p className="mt-2 text-sm text-slate-600 leading-relaxed">
                Store and manage your resume securely.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="flex flex-col items-start text-left bg-slate-50/50 p-8 rounded-2xl border border-slate-100/50 hover:shadow-md transition-shadow">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 mb-6">
                <User className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Build Your Profile</h3>
              <p className="mt-2 text-sm text-slate-600 leading-relaxed">
                Add your personal and professional details.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="flex flex-col items-start text-left bg-slate-50/50 p-8 rounded-2xl border border-slate-100/50 hover:shadow-md transition-shadow">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#f3e8ff] text-[#7c3aed] mb-6">
                <Target className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Get Job Ready</h3>
              <p className="mt-2 text-sm text-slate-600 leading-relaxed">
                Keep everything in one place for your job search.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Call to Action Banner */}
      <section id="about" className="bg-[#f8faff] border-t border-slate-100 py-16 text-center">
        <div className="mx-auto max-w-3xl px-4 sm:px-6">
          <h2 className="text-3xl font-extrabold text-slate-900 tracking-tight sm:text-4xl">
            Ready to take the next step?
          </h2>
          <p className="mt-4 text-base text-slate-600 max-w-xl mx-auto leading-relaxed">
            Create your account today and start your job search journey with JobPilot.
          </p>
          <div className="mt-8 flex justify-center">
            <button
              onClick={() => onNavigate('register')}
              className="flex items-center gap-2 rounded-lg bg-blue-600 py-3.5 px-8 text-base font-bold text-white hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all hover:translate-x-0.5"
            >
              Sign Up Now
              <ArrowRight className="h-5 w-5" />
            </button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-100 py-8 text-center text-xs text-slate-500">
        <div className="mx-auto max-w-7xl px-4 flex flex-col sm:flex-row justify-between items-center gap-4">
          <p>&copy; {new Date().getFullYear()} JobPilot AI. All rights reserved.</p>
          <p className="text-slate-400">Made with IBM Bob</p>
        </div>
      </footer>
    </div>
  );
}
