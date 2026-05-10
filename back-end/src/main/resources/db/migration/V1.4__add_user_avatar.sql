ALTER TABLE public.users ADD COLUMN IF NOT EXISTS avatar text;
COMMENT ON COLUMN public.users.avatar IS 'Base64-encoded user avatar (data URL format: data:image/png;base64,...). Distinct from logo, which is the company logo used in authorization templates.';
