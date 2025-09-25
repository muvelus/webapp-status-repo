import React, { useState, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card'
import { Button } from '../ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { Badge } from '../ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs'
import { Progress } from '../ui/progress'
import { Avatar, AvatarFallback } from '../ui/avatar'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts'
import { 
  Users, 
  TrendingUp, 
  Calendar, 
  FileText, 
  Activity,
  Download,
  RefreshCw
} from 'lucide-react'
import { User, WorkSummary, MeetingMinutes, TimeFilter } from '../../types'
import { apiClient } from '../../utils/api'
import { toast } from 'sonner'

const TeamView: React.FC = () => {
  const { user } = useAuth()
  const [teamMembers, setTeamMembers] = useState<User[]>([])
  const [selectedMember, setSelectedMember] = useState<User | null>(null)
  const [memberSummaries, setMemberSummaries] = useState<WorkSummary[]>([])
  const [teamMeetings, setTeamMeetings] = useState<MeetingMinutes[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedTimeFilter, setSelectedTimeFilter] = useState('week')

  const timeFilters: TimeFilter[] = [
    {
      label: 'This Week',
      value: 'week',
      startDate: new Date(new Date().setDate(new Date().getDate() - 7)),
      endDate: new Date()
    },
    {
      label: 'Last 2 Weeks',
      value: '2weeks',
      startDate: new Date(new Date().setDate(new Date().getDate() - 14)),
      endDate: new Date()
    },
    {
      label: 'Last Month',
      value: 'month',
      startDate: new Date(new Date().setMonth(new Date().getMonth() - 1)),
      endDate: new Date()
    }
  ]

  useEffect(() => {
    fetchTeamData()
  }, [selectedTimeFilter])

  useEffect(() => {
    if (selectedMember) {
      fetchMemberSummaries(selectedMember.id)
    }
  }, [selectedMember, selectedTimeFilter])

  const fetchTeamData = async () => {
    try {
      setLoading(true)
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const [membersResponse, meetingsResponse] = await Promise.all([
        apiClient.get<{ users: User[] }>('/api/users/team-members'),
        apiClient.get<{ meetings: MeetingMinutes[] }>(
          `/api/meetings/team-meetings?startDate=${filter.startDate.toISOString().split('T')[0]}&endDate=${filter.endDate.toISOString().split('T')[0]}`
        )
      ])

      setTeamMembers(membersResponse.users || [])
      setTeamMeetings(meetingsResponse.meetings || [])
      
      if (membersResponse.users && membersResponse.users.length > 0 && !selectedMember) {
        setSelectedMember(membersResponse.users[0])
      }
    } catch (error) {
      console.error('Failed to fetch team data:', error)
      toast.error('Failed to load team data')
    } finally {
      setLoading(false)
    }
  }

  const fetchMemberSummaries = async (userId: number) => {
    try {
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const response = await apiClient.get<{ summaries: WorkSummary[] }>(
        `/api/work-summaries/user/${userId}?startDate=${filter.startDate.toISOString()}&endDate=${filter.endDate.toISOString()}`
      )
      
      setMemberSummaries(response.summaries || [])
    } catch (error) {
      console.error('Failed to fetch member summaries:', error)
      toast.error('Failed to load member summaries')
    }
  }

  const generateTeamReport = async () => {
    try {
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const response = await apiClient.post<{ report: string }>('/api/work-summaries/generate-team-report', {
        startDate: filter.startDate.toISOString(),
        endDate: filter.endDate.toISOString()
      })

      const blob = new Blob([response.report], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `team-report-${selectedTimeFilter}-${new Date().toISOString().split('T')[0]}.txt`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)

      toast.success('Team report generated and downloaded!')
    } catch (error) {
      console.error('Failed to generate team report:', error)
      toast.error('Failed to generate team report')
    }
  }

  const getTeamStats = () => {
    const totalMembers = teamMembers.length
    const avgProductivity = teamMembers.reduce((acc, member) => {
      const memberAvg = memberSummaries
        .filter(s => s.userId === member.id)
        .reduce((sum, s) => sum + s.productivityScore, 0) / 
        (memberSummaries.filter(s => s.userId === member.id).length || 1)
      return acc + memberAvg
    }, 0) / (totalMembers || 1)

    const avgCollaboration = teamMembers.reduce((acc, member) => {
      const memberAvg = memberSummaries
        .filter(s => s.userId === member.id)
        .reduce((sum, s) => sum + s.collaborationScore, 0) / 
        (memberSummaries.filter(s => s.userId === member.id).length || 1)
      return acc + memberAvg
    }, 0) / (totalMembers || 1)

    const totalMeetings = teamMeetings.length

    return {
      totalMembers,
      avgProductivity: Math.round(avgProductivity),
      avgCollaboration: Math.round(avgCollaboration),
      totalMeetings
    }
  }

  const getProductivityData = () => {
    return teamMembers.map(member => {
      const summaries = memberSummaries.filter(s => s.userId === member.id)
      const avgProductivity = summaries.reduce((sum, s) => sum + s.productivityScore, 0) / (summaries.length || 1)
      const avgCollaboration = summaries.reduce((sum, s) => sum + s.collaborationScore, 0) / (summaries.length || 1)
      
      return {
        name: `${member.firstName} ${member.lastName}`,
        productivity: Math.round(avgProductivity),
        collaboration: Math.round(avgCollaboration)
      }
    })
  }

  const stats = getTeamStats()

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  if (!user || (user.role !== 'MANAGER' && user.role !== 'LEADER' && user.role !== 'ADMIN')) {
    return (
      <div className="text-center py-12">
        <Users className="mx-auto h-12 w-12 text-gray-400" />
        <h3 className="mt-2 text-sm font-medium text-gray-900">Access Denied</h3>
        <p className="mt-1 text-sm text-gray-500">
          You don't have permission to view team data
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Team View</h1>
          <p className="text-gray-600">Monitor your team's performance and collaboration</p>
        </div>
        <div className="flex space-x-3">
          <Select value={selectedTimeFilter} onValueChange={setSelectedTimeFilter}>
            <SelectTrigger className="w-48">
              <SelectValue placeholder="Select time period" />
            </SelectTrigger>
            <SelectContent>
              {timeFilters.map((filter) => (
                <SelectItem key={filter.value} value={filter.value}>
                  {filter.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={generateTeamReport}>
            <Download className="mr-2 h-4 w-4" />
            Generate Report
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Team Members</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalMembers}</div>
            <p className="text-xs text-muted-foreground">
              Active team members
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Productivity</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.avgProductivity}%</div>
            <Progress value={stats.avgProductivity} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Collaboration</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.avgCollaboration}%</div>
            <Progress value={stats.avgCollaboration} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Team Meetings</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalMeetings}</div>
            <p className="text-xs text-muted-foreground">
              This period
            </p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview">Team Overview</TabsTrigger>
          <TabsTrigger value="individual">Individual Performance</TabsTrigger>
          <TabsTrigger value="meetings">Team Meetings</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Team Performance Comparison</CardTitle>
              <CardDescription>
                Productivity and collaboration scores across team members
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={getProductivityData()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="productivity" fill="#8884d8" />
                  <Bar dataKey="collaboration" fill="#82ca9d" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Team Members</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {teamMembers.map((member) => {
                    const summaries = memberSummaries.filter(s => s.userId === member.id)
                    const avgProductivity = summaries.reduce((sum, s) => sum + s.productivityScore, 0) / (summaries.length || 1)
                    const avgCollaboration = summaries.reduce((sum, s) => sum + s.collaborationScore, 0) / (summaries.length || 1)
                    
                    return (
                      <div key={member.id} className="flex items-center justify-between p-3 border rounded-lg">
                        <div className="flex items-center space-x-3">
                          <Avatar>
                            <AvatarFallback>
                              {member.firstName[0]}{member.lastName[0]}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="font-medium">{member.firstName} {member.lastName}</p>
                            <p className="text-sm text-gray-600">{member.email}</p>
                          </div>
                        </div>
                        <div className="flex space-x-2">
                          <Badge variant="outline">
                            P: {Math.round(avgProductivity)}%
                          </Badge>
                          <Badge variant="outline">
                            C: {Math.round(avgCollaboration)}%
                          </Badge>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Recent Team Activity</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {teamMeetings.slice(0, 5).map((meeting) => (
                    <div key={meeting.id} className="flex items-center justify-between p-3 border rounded-lg">
                      <div>
                        <p className="font-medium text-sm">{meeting.meetingTitle}</p>
                        <p className="text-sm text-gray-600">
                          {new Date(meeting.meetingDate).toLocaleDateString()} • {meeting.durationMinutes}m
                        </p>
                      </div>
                      <Badge variant={meeting.processed ? 'default' : 'secondary'}>
                        {meeting.processed ? 'Processed' : 'Pending'}
                      </Badge>
                    </div>
                  ))}
                  {teamMeetings.length === 0 && (
                    <p className="text-gray-500 text-center py-4">
                      No team meetings found for this period
                    </p>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="individual" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Select Team Member</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {teamMembers.map((member) => (
                    <div
                      key={member.id}
                      className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                        selectedMember?.id === member.id ? 'bg-blue-50 border-blue-200' : 'hover:bg-gray-50'
                      }`}
                      onClick={() => setSelectedMember(member)}
                    >
                      <div className="flex items-center space-x-3">
                        <Avatar>
                          <AvatarFallback>
                            {member.firstName[0]}{member.lastName[0]}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <p className="font-medium">{member.firstName} {member.lastName}</p>
                          <p className="text-sm text-gray-600">{member.role}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>
                  {selectedMember ? `${selectedMember.firstName} ${selectedMember.lastName}` : 'Select a Member'}
                </CardTitle>
                <CardDescription>
                  Individual performance metrics and summaries
                </CardDescription>
              </CardHeader>
              <CardContent>
                {selectedMember ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm font-medium text-gray-600">Productivity Score</p>
                        <div className="text-2xl font-bold">
                          {Math.round(memberSummaries.reduce((sum, s) => sum + s.productivityScore, 0) / (memberSummaries.length || 1))}%
                        </div>
                        <Progress 
                          value={memberSummaries.reduce((sum, s) => sum + s.productivityScore, 0) / (memberSummaries.length || 1)} 
                          className="mt-2" 
                        />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-600">Collaboration Score</p>
                        <div className="text-2xl font-bold">
                          {Math.round(memberSummaries.reduce((sum, s) => sum + s.collaborationScore, 0) / (memberSummaries.length || 1))}%
                        </div>
                        <Progress 
                          value={memberSummaries.reduce((sum, s) => sum + s.collaborationScore, 0) / (memberSummaries.length || 1)} 
                          className="mt-2" 
                        />
                      </div>
                    </div>

                    <div>
                      <h4 className="font-medium mb-2">Recent Work Summaries</h4>
                      <div className="space-y-2">
                        {memberSummaries.slice(0, 3).map((summary) => (
                          <div key={summary.id} className="p-3 border rounded-lg">
                            <div className="flex justify-between items-start">
                              <div>
                                <p className="font-medium text-sm">{summary.summaryType} Summary</p>
                                <p className="text-sm text-gray-600">
                                  {new Date(summary.summaryDate).toLocaleDateString()}
                                </p>
                              </div>
                              <div className="flex space-x-2">
                                <Badge variant="outline" className="text-xs">
                                  P: {summary.productivityScore}%
                                </Badge>
                                <Badge variant="outline" className="text-xs">
                                  C: {summary.collaborationScore}%
                                </Badge>
                              </div>
                            </div>
                          </div>
                        ))}
                        {memberSummaries.length === 0 && (
                          <p className="text-gray-500 text-center py-4">
                            No summaries found for this member
                          </p>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <Users className="mx-auto h-12 w-12 text-gray-400" />
                    <h3 className="mt-2 text-sm font-medium text-gray-900">No member selected</h3>
                    <p className="mt-1 text-sm text-gray-500">
                      Choose a team member to view their performance
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="meetings" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Team Meetings</CardTitle>
              <CardDescription>
                Recent meetings involving your team members
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {teamMeetings.map((meeting) => (
                  <div key={meeting.id} className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="flex-1">
                      <h4 className="font-medium">{meeting.meetingTitle}</h4>
                      <p className="text-sm text-gray-600">
                        {new Date(meeting.meetingDate).toLocaleDateString()} • {meeting.durationMinutes} min
                      </p>
                      <p className="text-sm text-gray-600 mt-1">
                        Attendees: {meeting.attendees}
                      </p>
                      <div className="flex items-center mt-2 space-x-2">
                        <Badge variant={meeting.processed ? 'default' : 'secondary'}>
                          {meeting.processed ? 'Processed' : 'Pending'}
                        </Badge>
                        <Badge variant="outline">
                          {meeting.meetingPlatform}
                        </Badge>
                      </div>
                    </div>
                  </div>
                ))}
                {teamMeetings.length === 0 && (
                  <p className="text-gray-500 text-center py-8">
                    No team meetings found for this period
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default TeamView
