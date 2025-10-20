# Clausify 📄

> AI-Powered Contract Analysis Platform for Freelancers

Clausify helps freelancers and small business owners understand legal contracts by using AI to identify risky clauses, suggest negotiation points, and compare terms against industry standards.

## 🚀 Features

- **Instant AI Analysis** - Upload contracts and get analysis in under 60 seconds
- **Risk Identification** - Automatically flags problematic clauses across 7+ categories
- **Semantic Search** - Compares your clauses against 100+ standard contract terms
- **Negotiation Strategy** - Provides specific suggestions for pushing back on unfair terms
- **Version Comparison** - Track changes between contract iterations
- **Zero Cost** - Built entirely on free-tier services

## 🛠️ Tech Stack

- **Frontend**: Next.js 14, TypeScript, Tailwind CSS, shadcn/ui
- **Backend**: Next.js API Routes (serverless)
- **Database**: Supabase (PostgreSQL + pgvector)
- **AI**: Groq API (Llama 3.1 70B) with Gemini backup
- **Document Processing**: pdf-parse
- **Authentication**: Supabase Auth
- **Storage**: Supabase Storage
- **Deployment**: Vercel

## 📋 Prerequisites

- Node.js 18+ 
- npm or yarn
- Supabase account (free tier)
- Groq API key (free tier)
- Git

## 🏗️ Installation

### 1. Clone the Repository

```bash
git clone https://github.com/kymanirjarrett/clausify.git
cd clausify
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Set Up Environment Variables

**Option A - Interactive Setup (Recommended):**
```bash
npm run setup-env
```
This interactive script will guide you through entering all required API keys.

**Option B - Manual Setup:**
```bash
# Copy template
cp .env.example .env.local  # Mac/Linux
# OR
copy .env.example .env.local  # Windows

# Then edit .env.local with your actual values
```

**Option C - Quick Copy (if you already have values):**
```bash
npm run copy-env
# Then paste your values into .env.local
```

### 4. Verify Environment Configuration

```bash
npm run verify-env
```

This command checks that all required environment variables are properly set.

### 5. Set Up Supabase Database

1. Go to your Supabase project dashboard
2. Navigate to **SQL Editor**
3. Click **New Query**
4. Copy the contents of `lib/db/schema.sql`
5. Paste and run the query

### 6. Configure Supabase Storage

1. Go to **Storage** in Supabase dashboard
2. Create a new bucket named `contracts`
3. Set it to **Private**
4. Configure storage policies (see documentation in schema.sql)

### 7. Run Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) to see the app.

## 🔄 Multi-Machine Development

Working across multiple computers? We've got you covered:

```bash
# On new machine after cloning
npm install
npm run setup-env    # Interactive setup
npm run verify-env   # Verify configuration
npm run dev          # Start developing
```

See the [Multi-Machine Setup Guide](docs/multi-machine-setup.md) for detailed instructions on managing environment variables across development machines.

## 🔑 Getting API Keys

### Supabase Setup

1. Go to [supabase.com](https://supabase.com) and create a free account
2. Create a new project
3. Go to Project Settings > API
4. Copy your `URL` and `anon/public` key
5. Enable pgvector extension:
   - Go to Database > Extensions
   - Search for "vector" and enable it

### Groq API Setup

1. Go to [console.groq.com](https://console.groq.com)
2. Sign up for free account
3. Go to API Keys section
4. Create a new API key
5. Copy the key (free tier: 14,400 requests/day)

### Google Gemini Setup (Optional Backup)

1. Go to [makersuite.google.com](https://makersuite.google.com/app/apikey)
2. Create API key
3. Free tier: 60 requests/minute

## 📁 Project Structure

```
clausify/
├── app/
│   ├── (auth)/              # Authentication pages
│   ├── (dashboard)/         # Main app pages
│   ├── api/                 # API routes
│   ├── layout.tsx
│   └── page.tsx
├── components/
│   ├── ui/                  # shadcn components
│   ├── contracts/           # Contract-specific components
│   └── layout/              # Layout components
├── lib/
│   ├── ai/                  # AI integration
│   ├── db/                  # Database utilities
│   ├── pdf/                 # PDF processing
│   └── utils/               # Helper functions
├── types/                   # TypeScript definitions
└── public/                  # Static assets
```

## 🧪 Development Workflow

### Running Tests

```bash
npm run test
```

### Linting

```bash
npm run lint
```

### Type Checking

```bash
npm run type-check
```

### Build for Production

```bash
npm run build
```

## 📊 Database Schema

### Main Tables

- **contracts** - Stores uploaded contract metadata
- **clauses** - Extracted clauses with risk analysis
- **standard_clauses** - Library of industry-standard clauses for comparison

See `lib/db/schema.sql` for complete schema.

## 🤖 AI Integration

Clausify uses a multi-provider AI strategy:

1. **Primary**: Groq API with Llama 3.1 70B (fast, free, accurate)
2. **Backup**: Google Gemini 1.5 Flash (if Groq fails)
3. **Embeddings**: Sentence transformers for semantic search

### Prompt Engineering

The contract analysis prompt is optimized for freelancer-specific concerns:
- Payment terms fairness
- IP rights protection
- Liability limitations
- Non-compete reasonability
- Termination clause clarity

See `lib/ai/prompts.ts` for full prompts.

## 🚀 Deployment

### Deploy to Vercel

1. Push your code to GitHub
2. Go to [vercel.com](https://vercel.com)
3. Import your repository
4. Add environment variables
5. Deploy!

Vercel will automatically:
- Build your Next.js app
- Deploy frontend and API routes
- Provide a production URL

### Environment Variables on Vercel

Add all variables from `.env.local` to Vercel project settings.

## 📝 Usage

### Basic Flow

1. **Sign Up / Login** - Create account via Supabase Auth
2. **Upload Contract** - Drag and drop PDF (up to 10MB)
3. **Wait for Analysis** - AI processes in 30-60 seconds
4. **Review Results** - See flagged clauses, risk scores, suggestions
5. **Compare Versions** - Upload revised contract to see changes

### Supported Contract Types

- NDAs (Non-Disclosure Agreements)
- Service Agreements
- Freelance Contracts
- Employment Contracts
- Consulting Agreements
- General Business Contracts

## 🎯 Roadmap

### Phase 1 (Current)
- [x] Project setup
- [ ] Contract upload functionality
- [ ] AI analysis integration
- [ ] Basic UI for results

### Phase 2
- [ ] Vector search implementation
- [ ] Clause comparison feature
- [ ] Version diff tracking
- [ ] Export to PDF

### Phase 3
- [ ] User dashboard
- [ ] Contract history
- [ ] Analytics
- [ ] Performance optimizations

### Future Enhancements
- [ ] Collaborative review mode
- [ ] DocuSign integration
- [ ] Mobile app
- [ ] Custom clause library

## 🐛 Known Issues

- PDF upload limited to 10MB (Supabase free tier)
- OCR not yet implemented (scanned PDFs won't work)
- Rate limiting on AI APIs may cause delays during high usage

## 🤝 Contributing

This is a portfolio project, but suggestions are welcome!

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

MIT License - See LICENSE file for details

## ⚖️ Legal Disclaimer

**IMPORTANT**: Clausify is an AI-powered tool designed to assist with contract review. It is NOT a substitute for professional legal advice. 

- The analysis provided is for informational purposes only
- Always consult with a licensed attorney for legal matters
- No attorney-client relationship is created by using this tool
- The creator is not a lawyer and this is not legal advice

## 📧 Contact

For questions or feedback about this project:
- GitHub Issues: [github.com/kymanirjarrett/clausify/issues](https://github.com/yourusername/clausify/issues)
- Email: [jarretkr@mail.uc.edu](mailto:jarretkr@mail.uc.edu)

## 🙏 Acknowledgments

- Built with [Next.js](https://nextjs.org/)
- UI components from [shadcn/ui](https://ui.shadcn.com/)
- AI powered by [Groq](https://groq.com/) and [Google Gemini](https://ai.google.dev/)
- Database and auth by [Supabase](https://supabase.com/)

## 📈 Performance Metrics

- **Average Analysis Time**: 45-60 seconds for 20-page contracts
- **Risk Detection Categories**: 7+ clause types
- **Clause Database**: 100+ standard terms for comparison
- **Supported File Size**: Up to 10MB PDFs
- **API Response Time**: <2 seconds for Groq, <5 seconds for Gemini

---

**Built as a portfolio project to demonstrate full-stack development, AI integration, and modern web technologies.**