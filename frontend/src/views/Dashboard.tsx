import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useAuthStore } from '../stores/authStore';
import { 
  Briefcase, 
  FileText, 
  Upload, 
  LogOut, 
  MapPin, 
  Phone, 
  Linkedin, 
  Github, 
  Globe, 
  Edit,
  Download,
  AlertCircle,
  Mail,
  Calendar,
  Building,
  ExternalLink,
  User,
  Sparkles,
  Plus,
  Trash2,
  DollarSign,
  Clock,
  Link2,
  Layers,
  ChevronRight,
  Send,
  Bookmark,
  BookmarkCheck,
  CheckCircle
} from 'lucide-react';

interface ProfileData {
  id: number;
  name: string;
  email: string;
  phone: string;
  location: string;
  headline: string;
  yearsOfExperience: number;
  currentCompany: string;
  currentRole: string;
  linkedinUrl: string;
  githubUrl: string;
  portfolioUrl: string;
  skills: string;
}

interface ResumeData {
  id: number;
  fileName: string;
  contentType: string;
  createdAt: string;
}

interface JobData {
  id: number;
  title: string;
  company: string;
  location: string;
  workStyle: 'REMOTE' | 'HYBRID' | 'ONSITE';
  description: string;
  sourceUrl?: string;
  salaryInfo?: string;
  experienceRequired?: string;
  skills?: string[];
  createdAt: string;
}

interface Message {
  id: string;
  sender: 'USER' | 'AI';
  content: string;
  jobs?: JobData[];
  timestamp: Date;
}

export default function Dashboard() {
  const queryClient = useQueryClient();
  const logoutStore = useAuthStore((state) => state.logout);
  const userName = useAuthStore((state) => state.userName);

  // Tabs: 'profile' | 'jobs' (JobPilot AI Chat) | 'saved'
  const [activeTab, setActiveTab] = useState<'profile' | 'jobs' | 'saved'>('jobs');

  // Chat state
  const [chatInput, setChatInput] = useState('');
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome',
      sender: 'AI',
      content: `Hello ${userName || 'there'}! I am JobPilot, your intelligent career assistant. I can search LinkedIn & Indeed, find opportunities matching your background, and analyze candidate-to-job compatibility. \n\nTry asking me: *"Find me Java Spring Boot jobs in Bangalore!"* or *"Show me junior remote Python openings."*`,
      timestamp: new Date()
    }
  ]);
  const [chatLoading, setChatLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Profile Edit / Create state
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [profileForm, setProfileFields] = useState({
    phone: '',
    location: '',
    headline: '',
    yearsOfExperience: 0,
    currentCompany: '',
    currentRole: '',
    linkedinUrl: '',
    githubUrl: '',
    portfolioUrl: '',
    skills: '',
  });

  // Resume Upload State
  const [resumeFile, setResumeFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState('');
  const [uploading, setUploading] = useState(false);

  // View / Detail State
  const [selectedJob, setSelectedJob] = useState<JobData | null>(null);
  const [isAddingJobManually, setIsAddingJobManually] = useState(false);
  
  // Tab within Job Details panel: 'analysis' | 'description'
  const [activeDetailTab, setActiveDetailTab] = useState<'analysis' | 'description'>('analysis');

  // Fetch detailed job requirements dynamically when selected
  const {
    data: activeJobDetails,
    isLoading: jobDetailsLoading,
    isFetching: jobDetailsFetching
  } = useQuery<JobData & { requirements?: any }>({
    queryKey: ['job-details', selectedJob?.id],
    queryFn: async () => {
      if (!selectedJob?.id) return null;
      const res = await api.get(`/jobs/${selectedJob.id}`);
      return res.data;
    },
    enabled: !!selectedJob?.id,
  });

  // Manual Job Form State
  const [jobForm, setJobForm] = useState({
    title: '',
    company: '',
    location: '',
    workStyle: 'REMOTE',
    description: '',
    sourceUrl: '',
    salaryInfo: '',
    experienceRequired: '',
  });

  // Scroll chat to bottom on new message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, chatLoading]);

  // Fetch Candidate Profile
  const { 
    data: profile, 
    isLoading: profileLoading 
  } = useQuery<ProfileData, any>({
    queryKey: ['profile'],
    queryFn: async () => {
      const res = await api.get('/profile');
      return res.data;
    },
    retry: false,
  });

  // Fetch Resumes
  const { 
    data: resumes = [], 
    isLoading: resumesLoading 
  } = useQuery<ResumeData[]>({
    queryKey: ['resumes'],
    queryFn: async () => {
      const res = await api.get('/resumes');
      return res.data;
    },
    enabled: !!profile,
  });

  // Fetch Saved Jobs from PostgreSQL
  const {
    data: savedJobs = [],
    isLoading: savedJobsLoading
  } = useQuery<JobData[]>({
    queryKey: ['jobs'],
    queryFn: async () => {
      const res = await api.get('/jobs');
      return res.data;
    },
    enabled: !!profile,
  });

  // Create Profile Mutation
  const createProfileMutation = useMutation({
    mutationFn: async (newProfile: typeof profileForm) => {
      const res = await api.post('/profile', newProfile);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setIsEditingProfile(false);
    },
  });

  // Update Profile Mutation
  const updateProfileMutation = useMutation({
    mutationFn: async (updatedProfile: typeof profileForm) => {
      const res = await api.put('/profile', updatedProfile);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setIsEditingProfile(false);
    },
  });

  // Profile Auto-Import Mutation
  const importProfileMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/profile/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
      alert('Onboarding Complete! Your profile has been automatically created and populated using JobPilot AI.');
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to auto-generate profile from resume.');
    }
  });

  // AI Resume Parsing Mutation
  const parseResumeMutation = useMutation({
    mutationFn: async (resumeId: number) => {
      const res = await api.post(`/resumes/${resumeId}/parse`);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      alert('AI Processing Complete! Your profile has been automatically populated with details parsed from your resume.');
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to parse resume with AI service.');
    }
  });

  // Delete Resume Mutation
  const deleteResumeMutation = useMutation({
    mutationFn: async (resumeId: number) => {
      await api.delete(`/resumes/${resumeId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to delete resume.');
    }
  });

  // Save/Create Job Target Mutation
  const saveJobMutation = useMutation({
    mutationFn: async (newJob: any) => {
      const res = await api.post('/jobs', newJob);
      return res.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      alert(`Success! Job "${data.title}" at ${data.company} saved under your Saved Targets.`);
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to save job posting.');
    }
  });

  // Delete Saved Job Target Mutation
  const deleteJobMutation = useMutation({
    mutationFn: async (jobId: number) => {
      await api.delete(`/jobs/${jobId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      setSelectedJob(null);
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to delete job posting.');
    }
  });

  const handleEditClick = () => {
    if (profile) {
      setProfileFields({
        phone: profile.phone || '',
        location: profile.location || '',
        headline: profile.headline || '',
        yearsOfExperience: profile.yearsOfExperience || 0,
        currentCompany: profile.currentCompany || '',
        currentRole: profile.currentRole || '',
        linkedinUrl: profile.linkedinUrl || '',
        githubUrl: profile.githubUrl || '',
        portfolioUrl: profile.portfolioUrl || '',
        skills: profile.skills || '',
      });
    }
    setIsEditingProfile(true);
  };

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (profile) {
      updateProfileMutation.mutate(profileForm);
    } else {
      createProfileMutation.mutate(profileForm);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (file.size > 10 * 1024 * 1024) {
        setUploadError('File exceeds 10MB limit.');
        return;
      }
      setResumeFile(file);
      setUploadError('');
    }
  };

  const handleResumeUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resumeFile) return;

    setUploading(true);
    setUploadError('');

    const formData = new FormData();
    formData.append('file', resumeFile);

    try {
      await api.post('/resumes', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      setResumeFile(null);
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
    } catch (err: any) {
      console.error(err);
      setUploadError(err.response?.data?.message || 'Failed to upload resume file.');
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (resumeId: number, fileName: string) => {
    try {
      const response = await api.get(`/resumes/${resumeId}/download`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      console.error('Download failed:', err);
    }
  };

  const handleManualJobSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    saveJobMutation.mutate(jobForm);
    setIsAddingJobManually(false);
    setJobForm({
      title: '',
      company: '',
      location: '',
      workStyle: 'REMOTE',
      description: '',
      sourceUrl: '',
      salaryInfo: '',
      experienceRequired: '',
    });
  };

  const handleAnalyzeMatch = (job: any) => {
    if (!profile) {
      alert('Please set up your Candidate Profile first under the "My Profile" tab before performing AI matches.');
      setActiveTab('profile');
      return;
    }
    
    const existing = savedJobs.find((saved) => saved.title === job.title && saved.company === job.company);
    if (existing) {
      setSelectedJob(existing);
      setActiveDetailTab('analysis');
      setActiveTab('saved'); // transition to Saved Targets pipeline
    } else {
      // Save to Targets on the fly
      saveJobMutation.mutate(job, {
        onSuccess: (savedData) => {
          setSelectedJob(savedData);
          setActiveDetailTab('analysis');
          setActiveTab('saved');
        }
      });
    }
  };

  const renderJobDetailsContent = () => {
    if (!selectedJob) return null;

    return (
      <div className="overflow-y-auto pr-1 flex-grow space-y-6">
        {/* Header Info */}
        <div className="border-b border-slate-100 pb-5 space-y-3">
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center rounded-lg bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-700 uppercase">
              {selectedJob.workStyle}
            </span>
            {jobDetailsFetching && (
              <span className="text-[10px] font-bold text-blue-600 animate-pulse bg-blue-50/50 px-2 py-0.5 rounded">
                AI parsing...
              </span>
            )}
          </div>
          <h3 className="text-xl font-extrabold text-[#0f172a]">{selectedJob.title}</h3>
          <p className="text-sm font-bold text-slate-600 flex items-center gap-1.5">
            <Building className="h-4.5 w-4.5 text-slate-400" />
            {selectedJob.company}
          </p>
        </div>

        {/* Quick Metrics */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="bg-slate-50/50 rounded-2xl border border-slate-100 p-3.5 flex items-start gap-3">
            <MapPin className="h-4 w-4 text-slate-400 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">Location</p>
              <p className="text-xs font-bold text-slate-700 mt-0.5">{selectedJob.location}</p>
            </div>
          </div>

          <div className="bg-slate-50/50 rounded-2xl border border-slate-100 p-3.5 flex items-start gap-3">
            <Clock className="h-4 w-4 text-slate-400 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">Experience</p>
              <p className="text-xs font-bold text-slate-700 mt-0.5">{selectedJob.experienceRequired || 'Not Specified'}</p>
            </div>
          </div>

          <div className="bg-slate-50/50 rounded-2xl border border-slate-100 p-3.5 flex items-start gap-3">
            <DollarSign className="h-4 w-4 text-slate-400 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">Salary</p>
              <p className="text-xs font-bold text-slate-700 mt-0.5">{selectedJob.salaryInfo || 'Not Specified'}</p>
            </div>
          </div>
        </div>

        {/* Direct Links */}
        {selectedJob.sourceUrl && (
          <div className="pt-3">
            <a
              href={selectedJob.sourceUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 text-xs font-bold text-blue-600 hover:text-blue-700"
            >
              <Link2 className="h-4.5 w-4.5" />
              View Original Job Post
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </div>
        )}

        {/* Tab Selection */}
        <div className="flex border-b border-slate-100 pb-2.5 gap-4 pt-4">
          <button
            onClick={() => setActiveDetailTab('analysis')}
            className={`pb-1 text-xs font-bold border-b-2 transition-all flex items-center gap-1.5 ${
              activeDetailTab === 'analysis'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            <Sparkles className="h-3.5 w-3.5" />
            AI Requirements Analysis
          </button>
          <button
            onClick={() => setActiveDetailTab('description')}
            className={`pb-1 text-xs font-bold border-b-2 transition-all flex items-center gap-1.5 ${
              activeDetailTab === 'description'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            Original Specifications
          </button>
        </div>

        {/* Tab Body */}
        {activeDetailTab === 'analysis' ? (
          jobDetailsLoading ? (
            <div className="py-12 text-center space-y-4">
              <div className="flex justify-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-600 animate-pulse border border-blue-100/30">
                  <Sparkles className="h-6 w-6 animate-spin text-blue-600" />
                </div>
              </div>
              <div className="space-y-1">
                <p className="text-xs font-extrabold text-[#0f172a] animate-pulse">Parsing description & extracting requirements...</p>
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">JobPilot AI NLP Engine</p>
              </div>
            </div>
          ) : activeJobDetails?.requirements ? (
            <div className="space-y-5 pt-2">
              {/* Required Skills */}
              <div className="space-y-2">
                <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Required Technical Skills</h4>
                <div className="flex flex-wrap gap-1.5">
                  {activeJobDetails.requirements.requiredSkills?.length > 0 ? (
                    activeJobDetails.requirements.requiredSkills.map((skill: string) => (
                      <span key={skill} className="rounded-lg bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700 border border-emerald-100/50">
                        {skill}
                      </span>
                    ))
                  ) : (
                    <span className="text-xs text-slate-400 italic font-semibold">None specified</span>
                  )}
                </div>
              </div>

              {/* Preferred Skills */}
              <div className="space-y-2">
                <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Preferred/Nice-to-Have Skills</h4>
                <div className="flex flex-wrap gap-1.5">
                  {activeJobDetails.requirements.preferredSkills?.length > 0 ? (
                    activeJobDetails.requirements.preferredSkills.map((skill: string) => (
                      <span key={skill} className="rounded-lg bg-blue-50/50 px-2.5 py-1 text-xs font-bold text-blue-700 border border-blue-100/50">
                        {skill}
                      </span>
                    ))
                  ) : (
                    <span className="text-xs text-slate-400 italic font-semibold">None specified</span>
                  )}
                </div>
              </div>

              {/* Education & Experience Breakdown */}
              <div className="grid grid-cols-2 gap-3.5">
                <div className="rounded-2xl border border-slate-100 p-4 bg-slate-50/20">
                  <p className="text-[9px] font-extrabold text-slate-400 uppercase tracking-widest">Min. Experience Required</p>
                  <p className="text-sm font-extrabold text-slate-700 mt-1">
                    {activeJobDetails.requirements.minimumExperience !== null && activeJobDetails.requirements.minimumExperience !== undefined
                      ? `${activeJobDetails.requirements.minimumExperience} Years`
                      : '0-2 Years'}
                  </p>
                </div>
                <div className="rounded-2xl border border-slate-100 p-4 bg-slate-50/20">
                  <p className="text-[9px] font-extrabold text-slate-400 uppercase tracking-widest">Educational Standards</p>
                  <p className="text-sm font-extrabold text-slate-700 mt-1 truncate" title={activeJobDetails.requirements.requiredEducation}>
                    {activeJobDetails.requirements.requiredEducation || "Bachelor's Degree"}
                  </p>
                </div>
              </div>

              {/* Core Responsibilities */}
              <div className="space-y-2">
                <h4 className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">Core Responsibilities</h4>
                <div className="space-y-2 bg-slate-50/20 rounded-2xl border border-slate-100 p-4">
                  {activeJobDetails.requirements.responsibilities?.length > 0 ? (
                    activeJobDetails.requirements.responsibilities.map((resp: string, index: number) => (
                      <div key={index} className="flex items-start gap-2.5 text-xs text-slate-600 font-semibold leading-relaxed">
                        <CheckCircle className="h-4.5 w-4.5 text-blue-500 flex-shrink-0 mt-0.5" />
                        <span>{resp}</span>
                      </div>
                    ))
                  ) : (
                    <span className="text-xs text-slate-400 italic font-semibold">No responsibilities extracted.</span>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="py-8 text-center text-slate-400 text-xs font-semibold italic">
              AI requirements parsing was skipped due to a temporary service error. Please try clicking "Analyze Match" again.
            </div>
          )
        ) : (
          /* FULL Specifications Original Block */
          <div className="pt-2 space-y-3">
            <div className="rounded-2xl border border-slate-100 bg-slate-50/30 p-5 text-xs text-slate-600 font-semibold whitespace-pre-wrap leading-relaxed max-h-[350px] overflow-y-auto font-sans">
              {selectedJob.description}
            </div>
          </div>
        )}
      </div>
    );
  };

  // Chat submit event
  const handleChatSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    const userMsgText = chatInput;
    setChatInput('');

    // Add user message locally
    const userMessage: Message = {
      id: Math.random().toString(),
      sender: 'USER',
      content: userMsgText,
      timestamp: new Date()
    };

    setMessages((prev) => [...prev, userMessage]);
    setChatLoading(true);

    try {
      // Query Spring Boot backend (which attaches profile context and routes to Python FastAPI AI service)
      const res = await api.post('/chat', { message: userMsgText });
      
      const aiMessage: Message = {
        id: Math.random().toString(),
        sender: 'AI',
        content: res.data.response,
        jobs: res.data.jobs || [],
        timestamp: new Date()
      };

      setMessages((prev) => [...prev, aiMessage]);
    } catch (err: any) {
      console.error('Chat failed:', err);
      const errorMessage: Message = {
        id: Math.random().toString(),
        sender: 'AI',
        content: 'I had trouble communicating with the server. Please verify both services are running and your GEMINI_API_KEY is active.',
        timestamp: new Date()
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setChatLoading(false);
    }
  };

  // Get user initial for Avatar
  const getInitial = () => {
    if (profile?.name) return profile.name.charAt(0).toUpperCase();
    if (userName) return userName.charAt(0).toUpperCase();
    return 'U';
  };

  return (
    <div className="min-h-screen bg-[#f8faff] flex flex-col font-sans">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between">
            {/* Logo */}
            <div className="flex items-center gap-2 cursor-pointer" onClick={() => setActiveTab('jobs')}>
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white shadow-blue-100 shadow-lg transform -rotate-12">
                <svg className="h-4 w-4 fill-current" viewBox="0 0 24 24">
                  <path d="M2 21l21-9L2 3v7l15 2-15 2v7z" />
                </svg>
              </div>
              <span className="text-xl font-bold tracking-tight text-[#0f172a]">
                Job<span className="text-blue-600">Pilot</span>
              </span>
            </div>

            {/* Nav Tabs */}
            <nav className="flex h-full items-center gap-6">
              <button 
                onClick={() => { setActiveTab('jobs'); setSelectedJob(null); setIsAddingJobManually(false); }}
                className={`flex h-full items-center border-b-2 px-1 text-sm font-bold transition-all ${
                  activeTab === 'jobs' 
                    ? 'border-blue-600 text-blue-600' 
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                <Sparkles className="mr-2 h-4 w-4" />
                JobPilot AI (Chat)
              </button>

              <button 
                onClick={() => { setActiveTab('profile'); setIsAddingJobManually(false); }}
                className={`flex h-full items-center border-b-2 px-1 text-sm font-bold transition-all ${
                  activeTab === 'profile' 
                    ? 'border-blue-600 text-blue-600' 
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                <User className="mr-2 h-4 w-4" />
                My Profile
              </button>
              
              <button 
                onClick={() => { setActiveTab('saved'); setSelectedJob(null); setIsAddingJobManually(false); }}
                className={`flex h-full items-center border-b-2 px-1 text-sm font-bold transition-all ${
                  activeTab === 'saved' 
                    ? 'border-blue-600 text-blue-600' 
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                <Briefcase className="mr-2 h-4 w-4" />
                Saved Targets
              </button>
            </nav>

            {/* Logout Action */}
            <div className="flex items-center gap-4">
              <button
                onClick={logoutStore}
                className="flex items-center gap-2 rounded-xl border border-slate-200 py-1.5 px-3.5 text-sm font-bold text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors"
              >
                <LogOut className="h-4 w-4" />
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Body */}
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 flex-grow flex flex-col">
        
        {/* ======================= JOBS (CONVERSATIONAL DISCOVERY) TAB ======================= */}
        {activeTab === 'jobs' && (
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-12 flex-grow h-[calc(100vh-12rem)] min-h-[500px]">
            {/* LEFT COLUMN: CHAT INTERFACE (Col span 7) */}
            <div className="lg:col-span-7 flex flex-col rounded-3xl border border-slate-100 bg-white shadow-xl shadow-slate-100/40 overflow-hidden h-full">
              {/* Chat Header */}
              <div className="bg-slate-50/50 border-b border-slate-100 py-4.5 px-6 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600 shadow-sm border border-blue-100/20">
                    <Sparkles className="h-5 w-5 animate-pulse" />
                  </div>
                  <div className="text-left">
                    <h2 className="text-sm font-extrabold text-[#0f172a] flex items-center gap-1.5">
                      JobPilot AI Chat
                      <span className="inline-block h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
                    </h2>
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">conversational job discovery</p>
                  </div>
                </div>
              </div>

              {/* Chat Messages Log */}
              <div className="flex-grow overflow-y-auto p-6 space-y-6">
                {messages.map((msg) => {
                  const isAI = msg.sender === 'AI';
                  return (
                    <div key={msg.id} className={`flex gap-4 ${isAI ? 'justify-start' : 'justify-end'}`}>
                      {isAI && (
                        <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white shadow-sm font-bold text-xs select-none">
                          JP
                        </div>
                      )}
                      
                      <div className="max-w-[85%] space-y-4">
                        {/* Text Message Bubble */}
                        <div className={`rounded-2xl p-4.5 text-sm font-medium whitespace-pre-wrap text-left leading-relaxed ${
                          isAI 
                            ? 'bg-slate-50 text-slate-700 border border-slate-100/50' 
                            : 'bg-blue-600 text-white shadow-md shadow-blue-100'
                        }`}>
                          {msg.content}
                        </div>

                        {/* Interactive Discovered Job Cards */}
                        {msg.jobs && msg.jobs.length > 0 && (
                          <div className="space-y-3 mt-4 text-left">
                            {msg.jobs.map((job) => {
                              const isAlreadySaved = savedJobs.some((saved) => saved.title === job.title && saved.company === job.company);
                              return (
                                <div key={job.id} className="rounded-2xl border border-slate-100 p-5 bg-white hover:border-blue-300 hover:shadow-lg hover:shadow-slate-100/50 transition-all space-y-4.5 shadow-sm">
                                  <div className="flex justify-between items-start gap-4">
                                    <div>
                                      <h3 className="font-bold text-slate-800 text-base">{job.title}</h3>
                                      <p className="text-xs font-bold text-slate-500 flex items-center gap-1.5 mt-1.5">
                                        <Building className="h-4 w-4 text-slate-400" />
                                        {job.company}
                                      </p>
                                    </div>

                                    {/* Quick Save Action */}
                                    <button 
                                      onClick={() => isAlreadySaved ? null : saveJobMutation.mutate(job)}
                                      disabled={isAlreadySaved}
                                      className={`rounded-xl p-2 border transition-all ${
                                        isAlreadySaved 
                                          ? 'border-emerald-200 bg-emerald-50 text-emerald-600 cursor-default'
                                          : 'border-slate-200 hover:border-blue-400 hover:bg-blue-50/50 text-slate-400 hover:text-blue-600'
                                      }`}
                                      title={isAlreadySaved ? 'Job Saved' : 'Save this Job Posting'}
                                    >
                                      {isAlreadySaved ? <BookmarkCheck className="h-4 w-4" /> : <Bookmark className="h-4 w-4" />}
                                    </button>
                                  </div>

                                  {/* Quick Metrics */}
                                  <div className="flex flex-wrap items-center gap-3">
                                    <span className="inline-flex items-center rounded-md bg-blue-50 px-2 py-0.5 text-[10px] font-bold text-blue-700 uppercase tracking-wide">
                                      {job.workStyle}
                                    </span>
                                    <span className="inline-flex items-center gap-1 text-[10px] font-bold text-slate-400 uppercase tracking-wide">
                                      <MapPin className="h-3.5 w-3.5" />
                                      {job.location}
                                    </span>
                                    {job.experienceRequired && (
                                      <span className="inline-flex items-center gap-1 text-[10px] font-bold text-slate-400 uppercase tracking-wide">
                                        <Clock className="h-3.5 w-3.5" />
                                        {job.experienceRequired}
                                      </span>
                                    )}
                                    {job.salaryInfo && (
                                      <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-600 uppercase tracking-wide bg-emerald-50 px-1.5 py-0.5 rounded">
                                        {job.salaryInfo}
                                      </span>
                                    )}
                                  </div>

                                  {/* Action Triggers */}
                                  <div className="flex items-center gap-2 pt-3 border-t border-slate-50">
                                    <button
                                      onClick={() => { setSelectedJob(job); setIsAddingJobManually(false); }}
                                      className="flex-grow rounded-xl bg-slate-100 hover:bg-blue-50 py-2.5 px-4 text-xs font-bold text-slate-700 hover:text-blue-600 transition-colors flex items-center justify-center gap-1.5"
                                    >
                                      View Details
                                    </button>
                                    <button
                                      onClick={() => handleAnalyzeMatch(job)}
                                      className="flex-grow rounded-xl bg-blue-600 hover:bg-blue-700 py-2.5 px-4 text-xs font-bold text-white shadow-md shadow-blue-100 transition-all flex items-center justify-center gap-1.5"
                                    >
                                      <Sparkles className="h-3.5 w-3.5" />
                                      Analyze Match
                                    </button>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}

                {/* AI Typing Loading Indicator */}
                {chatLoading && (
                  <div className="flex gap-4 justify-start">
                    <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white shadow-sm font-bold text-xs select-none">
                      JP
                    </div>
                    <div className="rounded-2xl p-4.5 bg-slate-50 border border-slate-100/50 flex items-center gap-1">
                      <div className="h-2 w-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '0ms' }} />
                      <div className="h-2 w-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '150ms' }} />
                      <div className="h-2 w-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '300ms' }} />
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Chat Input Bar Form */}
              <form onSubmit={handleChatSubmit} className="border-t border-slate-100 p-4 flex gap-3">
                <input
                  type="text"
                  required
                  disabled={chatLoading}
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="Ask JobPilot to find jobs, analyze experience, or optimize details..."
                  className="flex-grow rounded-xl border border-slate-200 py-3 px-4.5 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:opacity-50 font-medium"
                />
                <button
                  type="submit"
                  disabled={chatLoading || !chatInput.trim()}
                  className="rounded-xl bg-blue-600 hover:bg-blue-700 p-3 text-white shadow-md shadow-blue-100 transition-all disabled:opacity-40"
                >
                  <Send className="h-4.5 w-4.5" />
                </button>
              </form>
            </div>

            {/* RIGHT COLUMN: DETAILED SPECIFICATIONS VIEW (Col span 5) */}
            <div className="lg:col-span-5 h-full overflow-hidden">
              {selectedJob ? (
                /* FULL DETAILS CARD VIEW */
                <div className="rounded-3xl border border-slate-100 bg-white p-7 shadow-xl shadow-slate-100/40 h-full flex flex-col text-left">
                  {renderJobDetailsContent()}
                </div>
              ) : (
                /* EMPTY RIGHT HAND DETAILS PANEL */
                <div className="rounded-3xl border border-dashed border-slate-200 bg-white p-12 text-center h-full flex flex-col justify-center shadow-xl shadow-slate-100/40">
                  <Briefcase className="mx-auto h-12 w-12 text-slate-300 stroke-1" />
                  <h3 className="mt-4 text-sm font-bold text-slate-700">Detailed Panel</h3>
                  <p className="mt-2 text-xs text-slate-400 font-semibold max-w-sm mx-auto leading-relaxed">
                    Select a discovered job card in the conversation feed and click "View Details" to preview full specifications here.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ======================= PROFILE TAB ======================= */}
        {activeTab === 'profile' && (
          <>
            {/* Title Block */}
            <div className="text-left mb-8">
              <h1 className="text-3xl font-extrabold text-[#0f172a] tracking-tight">My Profile</h1>
              <p className="text-sm text-slate-500 mt-1.5">Manage your personal and professional information</p>
            </div>

            <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
              {/* PROFILE CARD & FORM CONTAINER */}
              <div className="lg:col-span-2 space-y-8">
                <div className="rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-100/40">
                  {profileLoading ? (
                    <div className="flex items-center justify-center py-12">
                      <svg className="h-8 w-8 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                    </div>
                  ) : isEditingProfile || (!profile && !profileLoading) ? (
                    // EDIT / CREATE FORM
                    <form onSubmit={handleProfileSubmit} className="space-y-6">
                      {!profile && (
                        <div className="space-y-6">
                          <div className="rounded-xl bg-blue-50/70 p-4 border border-blue-100 text-sm text-blue-800 flex items-start gap-3 text-left">
                            <AlertCircle className="h-5 w-5 flex-shrink-0 mt-0.5" />
                            <div>
                              <p className="font-bold">Welcome to JobPilot!</p>
                              <p className="mt-0.5 text-xs text-blue-600 font-medium">Please create your professional profile first before uploading resumes or adding jobs.</p>
                            </div>
                          </div>

                          {/* ONE-CLICK IMPORT WIDGET */}
                          <div className="rounded-2xl border border-blue-100 bg-blue-50/20 p-6 text-left space-y-4">
                            <div className="flex items-center gap-2">
                              <Sparkles className="h-5 w-5 text-blue-600 animate-pulse" />
                              <h3 className="text-sm font-bold text-blue-900">🚀 One-Click Resume Onboarding</h3>
                            </div>
                            <p className="text-xs text-slate-500 font-semibold leading-relaxed">
                              Upload your resume (PDF or DOCX) to automatically generate your professional candidate profile using JobPilot AI in seconds!
                            </p>

                            <div className="group relative flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-blue-300 hover:border-blue-600 transition-colors cursor-pointer bg-white py-6 px-4">
                              <input
                                type="file"
                                accept=".pdf,.docx"
                                onChange={(e) => {
                                  if (e.target.files && e.target.files[0]) {
                                    importProfileMutation.mutate(e.target.files[0]);
                                  }
                                }}
                                disabled={importProfileMutation.isPending}
                                className="absolute inset-0 h-full w-full opacity-0 cursor-pointer disabled:cursor-not-allowed"
                              />
                              {importProfileMutation.isPending ? (
                                <div className="flex flex-col items-center gap-2">
                                  <svg className="h-8 w-8 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                  </svg>
                                  <span className="text-xs text-blue-600 font-bold animate-pulse">Generating profile using JobPilot AI...</span>
                                </div>
                              ) : (
                                <>
                                  <Upload className="h-7 w-7 text-blue-500 group-hover:text-blue-700 transition-colors" />
                                  <span className="mt-2 text-xs text-slate-700 font-bold">Select Resume to Import & Populate Profile</span>
                                  <span className="mt-1 text-[10px] text-slate-400">PDF, DOCX up to 10MB</span>
                                </>
                              )}
                            </div>

                            <div className="flex items-center gap-3">
                              <div className="h-px bg-slate-200 flex-grow" />
                              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">or fill manually below</span>
                              <div className="h-px bg-slate-200 flex-grow" />
                            </div>
                          </div>
                        </div>
                      )}

                      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 text-left">
                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Headline</label>
                          <input
                            type="text"
                            required
                            value={profileForm.headline}
                            onChange={(e) => setProfileFields({ ...profileForm, headline: e.target.value })}
                            placeholder="Software Engineer"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Years of Experience</label>
                          <input
                            type="number"
                            min="0"
                            max="50"
                            required
                            value={profileForm.yearsOfExperience}
                            onChange={(e) => setProfileFields({ ...profileForm, yearsOfExperience: parseInt(e.target.value) || 0 })}
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Location</label>
                          <input
                            type="text"
                            required
                            value={profileForm.location}
                            onChange={(e) => setProfileFields({ ...profileForm, location: e.target.value })}
                            placeholder="Bangalore, Karnataka"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Phone Number</label>
                          <input
                            type="text"
                            required
                            value={profileForm.phone}
                            onChange={(e) => setProfileFields({ ...profileForm, phone: e.target.value })}
                            placeholder="+91 98765 43210"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Current Company</label>
                          <input
                            type="text"
                            value={profileForm.currentCompany}
                            onChange={(e) => setProfileFields({ ...profileForm, currentCompany: e.target.value })}
                            placeholder="IBM"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Current Role</label>
                          <input
                            type="text"
                            value={profileForm.currentRole}
                            onChange={(e) => setProfileFields({ ...profileForm, currentRole: e.target.value })}
                            placeholder="Software Engineer"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">LinkedIn URL</label>
                          <input
                            type="url"
                            value={profileForm.linkedinUrl}
                            onChange={(e) => setProfileFields({ ...profileForm, linkedinUrl: e.target.value })}
                            placeholder="https://linkedin.com/in/amitkumar"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">GitHub URL</label>
                          <input
                            type="url"
                            value={profileForm.githubUrl}
                            onChange={(e) => setProfileFields({ ...profileForm, githubUrl: e.target.value })}
                            placeholder="https://github.com/amitkumar"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div className="sm:col-span-2">
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Portfolio Website URL</label>
                          <input
                            type="url"
                            value={profileForm.portfolioUrl}
                            onChange={(e) => setProfileFields({ ...profileForm, portfolioUrl: e.target.value })}
                            placeholder="https://myportfolio.com"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>

                        <div className="sm:col-span-2">
                          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">
                            Skills
                            <span className="ml-1 normal-case font-normal text-slate-400">(comma-separated)</span>
                          </label>
                          <input
                            type="text"
                            value={profileForm.skills}
                            onChange={(e) => setProfileFields({ ...profileForm, skills: e.target.value })}
                            placeholder="Java, Spring Boot, React, PostgreSQL, Docker"
                            className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          />
                        </div>
                      </div>

                      <div className="flex justify-end gap-3 pt-6 border-t border-slate-100">
                        {profile && (
                          <button
                            type="button"
                            onClick={() => setIsEditingProfile(false)}
                            className="rounded-xl border border-slate-200 py-2.5 px-5 text-sm font-bold text-slate-600 hover:bg-slate-50"
                          >
                            Cancel
                          </button>
                        )}
                        <button
                          type="submit"
                          disabled={createProfileMutation.isPending || updateProfileMutation.isPending}
                          className="rounded-xl bg-blue-600 py-2.5 px-6 text-sm font-bold text-white hover:bg-blue-700 shadow-md shadow-blue-200 transition-colors"
                        >
                          {createProfileMutation.isPending || updateProfileMutation.isPending ? 'Saving...' : 'Save Profile'}
                        </button>
                      </div>
                    </form>
                  ) : (
                    // GORGEOUS PROFILE VIEW
                    <div className="space-y-8 text-left">
                      {/* Avatar & Headline block */}
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 border-b border-slate-100 pb-6">
                        <div className="flex items-center gap-5">
                          {/* Avatar Circle */}
                          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-blue-50 text-blue-600 text-3xl font-extrabold shadow-inner border border-blue-100/30">
                            {getInitial()}
                          </div>
                          <div>
                            <h2 className="text-2xl font-extrabold text-[#0f172a]">{profile?.name}</h2>
                            <p className="text-sm font-semibold text-slate-500 mt-1">{profile?.headline}</p>
                          </div>
                        </div>
                        {/* Edit button */}
                        <button
                          onClick={handleEditClick}
                          className="flex items-center justify-center gap-2 rounded-xl border border-blue-600/20 py-2 px-5 text-xs font-bold text-blue-600 bg-blue-50/30 hover:bg-blue-50 transition-colors self-start sm:self-center"
                        >
                          <Edit className="h-4 w-4" />
                          Edit Profile
                        </button>
                      </div>

                      {/* Biographical list */}
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div className="flex items-center gap-3.5 text-slate-600 font-semibold text-sm">
                          <div className="text-slate-400">
                            <Mail className="h-5 w-5" />
                          </div>
                          <span>{profile?.email}</span>
                        </div>

                        <div className="flex items-center gap-3.5 text-slate-600 font-semibold text-sm">
                          <div className="text-slate-400">
                            <Phone className="h-5 w-5" />
                          </div>
                          <span>{profile?.phone}</span>
                        </div>

                        <div className="flex items-center gap-3.5 text-slate-600 font-semibold text-sm">
                          <div className="text-slate-400">
                            <MapPin className="h-5 w-5" />
                          </div>
                          <span>{profile?.location}</span>
                        </div>

                        <div className="flex items-center gap-3.5 text-slate-600 font-semibold text-sm">
                          <div className="text-slate-400">
                            <Calendar className="h-5 w-5" />
                          </div>
                          <span>
                            {profile?.yearsOfExperience === 1 
                              ? '1 year of experience' 
                              : `${profile?.yearsOfExperience} years of experience`}
                          </span>
                        </div>
                      </div>

                      {/* Current Role / Company */}
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-slate-100">
                        <div className="bg-slate-50/50 rounded-2xl border border-slate-100 p-4.5 flex items-start gap-4">
                          <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                            <Briefcase className="h-5.5 w-5.5" />
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Current Role</p>
                            <p className="text-sm font-bold text-slate-800 mt-1">
                              {profile?.currentRole || 'Not Employed'}
                            </p>
                          </div>
                        </div>

                        <div className="bg-slate-50/50 rounded-2xl border border-slate-100 p-4.5 flex items-start gap-4">
                          <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                            <Building className="h-5.5 w-5.5" />
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Company</p>
                            <p className="text-sm font-bold text-slate-800 mt-1">
                              {profile?.currentCompany || 'Not Employed'}
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* Skills */}
                      {profile?.skills && (
                        <div className="pt-6 border-t border-slate-100 space-y-3">
                          <h3 className="text-sm font-bold text-[#0f172a] flex items-center gap-2">
                            <svg style={{ width: '16px', height: '16px', flexShrink: 0 }} className="text-blue-600" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                            </svg>
                            Skills
                          </h3>
                          <div className="flex flex-wrap gap-2">
                            {profile.skills.split(',').map((skill) => skill.trim()).filter(Boolean).map((skill) => (
                              <span
                                key={skill}
                                className="inline-flex items-center rounded-lg bg-blue-50 px-3 py-1 text-xs font-bold text-blue-700 border border-blue-100"
                              >
                                {skill}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Links */}
                      <div className="pt-6 border-t border-slate-100 space-y-4">
                        <h3 className="text-sm font-bold text-[#0f172a] flex items-center gap-2">
                          <svg style={{ width: '16px', height: '16px', flexShrink: 0 }} className="text-blue-600" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                          </svg>
                          Professional Links
                        </h3>

                        {!profile?.linkedinUrl && !profile?.githubUrl && !profile?.portfolioUrl ? (
                          <p className="text-xs text-slate-400 italic">No links found in resume. Edit your profile to add links.</p>
                        ) : (
                        <div className="space-y-2">
                          {profile?.linkedinUrl && (
                            <a
                              href={profile.linkedinUrl.startsWith('http') ? profile.linkedinUrl : `https://${profile.linkedinUrl}`}
                              target="_blank"
                              rel="noreferrer"
                              className="flex items-center justify-between rounded-xl border border-slate-100 p-3.5 bg-slate-50/30 hover:bg-slate-50 hover:border-slate-200 transition-all text-xs font-bold text-slate-700 hover:text-blue-600"
                            >
                              <span className="flex items-center gap-3">
                                <Linkedin className="h-4 w-4 text-blue-600 fill-current flex-shrink-0" />
                                {profile.linkedinUrl.replace(/^https?:\/\//, '')}
                              </span>
                              <ExternalLink className="h-3.5 w-3.5 text-slate-400 flex-shrink-0" />
                            </a>
                          )}

                          {profile?.githubUrl && (
                            <a
                              href={profile.githubUrl.startsWith('http') ? profile.githubUrl : `https://${profile.githubUrl}`}
                              target="_blank"
                              rel="noreferrer"
                              className="flex items-center justify-between rounded-xl border border-slate-100 p-3.5 bg-slate-50/30 hover:bg-slate-50 hover:border-slate-200 transition-all text-xs font-bold text-slate-700 hover:text-slate-950"
                            >
                              <span className="flex items-center gap-3">
                                <Github className="h-4 w-4 text-slate-900 flex-shrink-0" />
                                {profile.githubUrl.replace(/^https?:\/\//, '')}
                              </span>
                              <ExternalLink className="h-3.5 w-3.5 text-slate-400 flex-shrink-0" />
                            </a>
                          )}

                          {profile?.portfolioUrl && (
                            <a
                              href={profile.portfolioUrl.startsWith('http') ? profile.portfolioUrl : `https://${profile.portfolioUrl}`}
                              target="_blank"
                              rel="noreferrer"
                              className="flex items-center justify-between rounded-xl border border-slate-100 p-3.5 bg-slate-50/30 hover:bg-slate-50 hover:border-slate-200 transition-all text-xs font-bold text-slate-700 hover:text-emerald-600"
                            >
                              <span className="flex items-center gap-3">
                                <Globe className="h-4 w-4 text-emerald-600 flex-shrink-0" />
                                {profile.portfolioUrl.replace(/^https?:\/\//, '')}
                              </span>
                              <ExternalLink className="h-3.5 w-3.5 text-slate-400 flex-shrink-0" />
                            </a>
                          )}
                        </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* RIGHT COLUMN: RESUME FILE MANAGER */}
              <div className="space-y-8 text-left">
                <div className="rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-100/40">
                  <div className="flex items-center gap-3 border-b border-slate-100 pb-5">
                    <FileText className="h-6 w-6 text-blue-600" />
                    <h2 className="text-xl font-bold text-[#0f172a]">Resume Files</h2>
                  </div>

                  {/* Upload Form Widget */}
                  {profile ? (
                    <form onSubmit={handleResumeUpload} className="mt-6 space-y-4">
                      <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">
                        Upload New Version (PDF/DOCX)
                      </label>
                      
                      <div className="group relative flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 px-6 py-8 hover:border-blue-500 transition-colors cursor-pointer bg-slate-50/30">
                        <input
                          type="file"
                          accept=".pdf,.docx"
                          onChange={handleFileChange}
                          className="absolute inset-0 h-full w-full opacity-0 cursor-pointer"
                        />
                        <Upload className="h-8 w-8 text-slate-400 group-hover:text-blue-500 transition-colors" />
                        <span className="mt-3 text-xs text-slate-600 font-bold">
                          {resumeFile ? resumeFile.name : 'Click or Drag & Drop'}
                        </span>
                        <span className="mt-1 text-[10px] text-slate-400">
                          PDF, DOCX up to 10MB
                        </span>
                      </div>

                      {uploadError && (
                        <div className="rounded-xl bg-rose-50 p-3 text-xs font-medium text-rose-800 border border-rose-100">
                          {uploadError}
                        </div>
                      )}

                      {resumeFile && (
                        <button
                          type="submit"
                          disabled={uploading}
                          className="w-full rounded-xl bg-blue-600 py-3 text-sm font-bold text-white hover:bg-blue-700 shadow-md shadow-blue-200 transition-all disabled:opacity-50"
                        >
                          {uploading ? 'Uploading...' : 'Submit File'}
                        </button>
                      )}
                    </form>
                  ) : (
                    <p className="mt-4 text-xs font-semibold text-slate-400 italic">
                      Create your Candidate Profile first to unlock resume uploads.
                    </p>
                  )}

                  {/* Uploaded Versions List */}
                  {profile && (
                    <div className="mt-8 space-y-4">
                      <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100 pb-2">
                        Uploaded Versions ({resumes.length})
                      </h3>

                      {resumesLoading ? (
                        <div className="flex justify-center py-4">
                          <svg className="h-5 w-5 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                          </svg>
                        </div>
                      ) : resumes.length === 0 ? (
                        <p className="text-xs font-semibold text-slate-400 italic">No resume versions uploaded yet.</p>
                      ) : (
                        <div className="space-y-3">
                          {resumes.map((resume) => (
                            <div 
                              key={resume.id}
                              className="flex items-center justify-between rounded-2xl border border-slate-100 p-4.5 bg-slate-50/20 hover:bg-slate-50 transition-colors"
                            >
                              <div className="min-w-0 pr-3">
                                <p className="truncate text-sm font-bold text-slate-800" title={resume.fileName}>
                                  {resume.fileName}
                                </p>
                                <p className="text-[10px] text-slate-500 font-semibold mt-0.5">
                                  Uploaded: {new Date(resume.createdAt).toLocaleDateString()}
                                </p>
                              </div>
                              <div className="flex items-center gap-2">
                                <button
                                  onClick={() => parseResumeMutation.mutate(resume.id)}
                                  disabled={parseResumeMutation.isPending}
                                  className="rounded-lg bg-blue-50 p-2 text-blue-600 hover:bg-blue-100 hover:text-blue-700 transition-colors disabled:opacity-50 flex items-center justify-center"
                                  title="Parse Resume with AI"
                                >
                                  {parseResumeMutation.isPending ? (
                                    <svg className="h-4.5 w-4.5 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
                                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                    </svg>
                                  ) : (
                                    <Sparkles className="h-4.5 w-4.5" />
                                  )}
                                </button>
                                <button
                                  onClick={() => handleDownload(resume.id, resume.fileName)}
                                  className="rounded-lg bg-slate-100 p-2 text-slate-600 hover:bg-blue-100 hover:text-blue-600 transition-colors"
                                  title="Download original file"
                                >
                                  <Download className="h-4.5 w-4.5" />
                                </button>
                                <button
                                  onClick={() => {
                                    if (confirm(`Delete "${resume.fileName}"? This cannot be undone.`)) {
                                      deleteResumeMutation.mutate(resume.id);
                                    }
                                  }}
                                  disabled={deleteResumeMutation.isPending}
                                  className="rounded-lg bg-rose-50 p-2 text-rose-500 hover:bg-rose-100 hover:text-rose-600 transition-colors disabled:opacity-50"
                                  title="Delete resume"
                                >
                                  <Trash2 className="h-4.5 w-4.5" />
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </>
        )}

        {/* ======================= SAVED TARGETS (TRACKER) TAB ======================= */}
        {activeTab === 'saved' && (
          <>
            {/* Title Block */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 text-left">
              <div>
                <h1 className="text-3xl font-extrabold text-[#0f172a] tracking-tight">Saved Targets</h1>
                <p className="text-sm text-slate-500 mt-1.5">Manage and track jobs you are targeting for applications</p>
              </div>
              
              {profile && (
                <button
                  onClick={() => { setIsAddingJobManually(true); setSelectedJob(null); }}
                  className="flex items-center justify-center gap-2 rounded-xl bg-blue-600 py-3 px-5 text-sm font-bold text-white hover:bg-blue-700 shadow-md shadow-blue-200 transition-colors self-start sm:self-center"
                >
                  <Plus className="h-4.5 w-4.5" />
                  Add Job Manually
                </button>
              )}
            </div>

            {!profile ? (
              <div className="rounded-3xl border border-dashed border-slate-200 bg-white p-12 text-center max-w-2xl mx-auto shadow-sm">
                <AlertCircle className="mx-auto h-12 w-12 text-slate-400" />
                <h3 className="mt-4 text-lg font-bold text-slate-800">Profile Required</h3>
                <p className="mt-2 text-sm text-slate-500">
                  You must set up your Candidate Profile first before managing jobs.
                </p>
                <button
                  onClick={() => setActiveTab('profile')}
                  className="mt-6 rounded-xl bg-blue-600 py-2.5 px-6 text-sm font-bold text-white hover:bg-blue-700 transition-colors"
                >
                  Go to Profile
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
                {/* LEFT COLUMN: LIST OF SAVED TARGETS */}
                <div className="space-y-4 text-left">
                  <div className="rounded-3xl border border-slate-100 bg-white p-6 shadow-xl shadow-slate-100/40">
                    <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider border-b border-slate-100 pb-3 mb-4 flex items-center justify-between">
                      My Pipeline
                      <span className="rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-600 font-bold">{savedJobs.length}</span>
                    </h3>

                    {savedJobsLoading ? (
                      <div className="flex justify-center py-12">
                        <svg className="h-8 w-8 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                        </svg>
                      </div>
                    ) : savedJobs.length === 0 ? (
                      <div className="py-8 text-center">
                        <p className="text-sm font-semibold text-slate-400 italic">No targets saved yet.</p>
                        <p className="text-xs text-slate-400 mt-1">Discovered jobs inside the AI Chat can be bookmarked here instantly!</p>
                      </div>
                    ) : (
                      <div className="space-y-3 max-h-[600px] overflow-y-auto pr-1">
                        {savedJobs.map((job) => {
                          const isSelected = selectedJob?.id === job.id;
                          return (
                            <button
                              key={job.id}
                              onClick={() => { setSelectedJob(job); setIsAddingJobManually(false); }}
                              className={`w-full text-left rounded-2xl border p-4.5 transition-all flex items-start justify-between gap-3 ${
                                isSelected 
                                  ? 'border-blue-600 bg-blue-50/20 ring-1 ring-blue-600' 
                                  : 'border-slate-100 bg-slate-50/10 hover:bg-slate-50/50 hover:border-slate-200'
                              }`}
                            >
                              <div className="min-w-0">
                                <h4 className="font-bold text-slate-800 truncate text-sm">{job.title}</h4>
                                <p className="text-xs font-semibold text-slate-500 mt-1 flex items-center gap-1.5">
                                  <Building className="h-3.5 w-3.5 text-slate-400" />
                                  <span className="truncate">{job.company}</span>
                                </p>
                                <div className="flex items-center gap-2 mt-2">
                                  <span className="inline-flex items-center rounded-md bg-blue-50 px-2 py-0.5 text-[10px] font-bold text-blue-700 uppercase">
                                    {job.workStyle}
                                  </span>
                                  <span className="text-[10px] font-semibold text-slate-400 truncate flex items-center gap-1">
                                    <MapPin className="h-3 w-3" />
                                    {job.location}
                                  </span>
                                </div>
                              </div>
                              <ChevronRight className="h-4.5 w-4.5 text-slate-400 self-center flex-shrink-0" />
                            </button>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>

                {/* RIGHT COLUMN: DETAIL CARD or FORM */}
                <div className="lg:col-span-2 text-left">
                  {isAddingJobManually ? (
                    /* MANUAL JOB ENTRY FORM */
                    <div className="rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-100/40">
                      <div className="flex items-center gap-3 border-b border-slate-100 pb-5 mb-6">
                        <Plus className="h-6 w-6 text-blue-600" />
                        <h2 className="text-xl font-bold text-[#0f172a]">Add Target Job Manually</h2>
                      </div>

                      <form onSubmit={handleManualJobSubmit} className="space-y-6">
                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                          <div className="sm:col-span-2">
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Job Title *</label>
                            <input
                              type="text"
                              required
                              value={jobForm.title}
                              onChange={(e) => setJobForm({ ...jobForm, title: e.target.value })}
                              placeholder="e.g. Senior Software Engineer"
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Company Name *</label>
                            <input
                              type="text"
                              required
                              value={jobForm.company}
                              onChange={(e) => setJobForm({ ...jobForm, company: e.target.value })}
                              placeholder="e.g. IBM"
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Work Style *</label>
                            <select
                              value={jobForm.workStyle}
                              onChange={(e) => setJobForm({ ...jobForm, workStyle: e.target.value })}
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            >
                              <option value="REMOTE">Remote</option>
                              <option value="HYBRID">Hybrid</option>
                              <option value="ONSITE">On-site</option>
                            </select>
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Location *</label>
                            <input
                              type="text"
                              required
                              value={jobForm.location}
                              onChange={(e) => setJobForm({ ...jobForm, location: e.target.value })}
                              placeholder="e.g. Bangalore, KA"
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Experience Required</label>
                            <input
                              type="text"
                              value={jobForm.experienceRequired}
                              onChange={(e) => setJobForm({ ...jobForm, experienceRequired: e.target.value })}
                              placeholder="e.g. 5+ years"
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Salary Information</label>
                            <input
                              type="text"
                              value={jobForm.salaryInfo}
                              onChange={(e) => setJobForm({ ...jobForm, salaryInfo: e.target.value })}
                              placeholder="e.g. $120,000 - $140,000"
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Job Source URL</label>
                            <input
                              type="url"
                              value={jobForm.sourceUrl}
                              onChange={(e) => setJobForm({ ...jobForm, sourceUrl: e.target.value })}
                              placeholder="e.g. https://careers.ibm.com/job/..."
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                          </div>

                          <div className="sm:col-span-2">
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">Full Job Description *</label>
                            <textarea
                              required
                              rows={8}
                              value={jobForm.description}
                              onChange={(e) => setJobForm({ ...jobForm, description: e.target.value })}
                              placeholder="Paste the complete job description text here..."
                              className="mt-2 block w-full rounded-xl border border-slate-200 py-3 px-4 text-sm bg-slate-50/50 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 font-sans"
                            />
                          </div>
                        </div>

                        <div className="flex justify-end gap-3 pt-6 border-t border-slate-100">
                          <button
                            type="button"
                            onClick={() => setIsAddingJobManually(false)}
                            className="rounded-xl border border-slate-200 py-2.5 px-5 text-sm font-bold text-slate-600 hover:bg-slate-50"
                          >
                            Cancel
                          </button>
                          <button
                            type="submit"
                            disabled={saveJobMutation.isPending}
                            className="rounded-xl bg-blue-600 py-2.5 px-6 text-sm font-bold text-white hover:bg-blue-700 shadow-md shadow-blue-200 transition-colors"
                          >
                            {saveJobMutation.isPending ? 'Saving...' : 'Save Job Target'}
                          </button>
                        </div>
                      </form>
                    </div>
                  ) : selectedJob ? (
                    /* SAVED TARGET JOB DETAIL VIEW */
                    <div className="rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-100/40 space-y-6 h-full flex flex-col text-left">
                      {/* Extra controls like Delete target */}
                      <div className="flex justify-end border-b border-slate-100 pb-4">
                        <button
                          onClick={() => {
                            if (confirm('Are you sure you want to delete this job posting target?')) {
                              deleteJobMutation.mutate(selectedJob.id);
                            }
                          }}
                          disabled={deleteJobMutation.isPending}
                          className="flex items-center justify-center gap-2 rounded-xl border border-rose-100 hover:border-rose-500 py-1.5 px-4 text-xs font-bold text-rose-600 hover:bg-rose-50 transition-colors disabled:opacity-50"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                          {deleteJobMutation.isPending ? 'Deleting...' : 'Delete Target'}
                        </button>
                      </div>
                      
                      {renderJobDetailsContent()}
                    </div>
                  ) : (
                    /* EMPTY STATE */
                    <div className="rounded-3xl border border-dashed border-slate-200 bg-white p-16 text-center shadow-xl shadow-slate-100/40">
                      <Briefcase className="mx-auto h-12 w-12 text-slate-300 stroke-1" />
                      <h3 className="mt-4 text-base font-bold text-slate-700">No Target Selected</h3>
                      <p className="mt-2 text-xs text-slate-400 font-semibold max-w-sm mx-auto leading-relaxed">
                        Select a target job posting from your pipeline on the left sidebar to preview its details, or click "Add Job Manually" to register a new role.
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </>
        )}

      </main>
    </div>
  );
}
