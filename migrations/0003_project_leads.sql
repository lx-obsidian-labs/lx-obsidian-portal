-- Project enquiries. Apply to the Pages D1 database before enabling the form.
CREATE TABLE IF NOT EXISTS project_leads (
 id TEXT PRIMARY KEY,
 created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 name TEXT NOT NULL,
 business_name TEXT,
 email TEXT NOT NULL,
 phone TEXT,
 industry TEXT,
 project_type TEXT NOT NULL,
 subject TEXT NOT NULL,
 message TEXT NOT NULL,
 budget TEXT,
 timeline TEXT,
 nda_requested INTEGER NOT NULL DEFAULT 0,
 source TEXT NOT NULL DEFAULT 'website',
 status TEXT NOT NULL DEFAULT 'new',
 notification_status TEXT NOT NULL DEFAULT 'not_configured'
);
CREATE INDEX IF NOT EXISTS idx_project_leads_created ON project_leads(created_at);
CREATE INDEX IF NOT EXISTS idx_project_leads_status ON project_leads(status);
